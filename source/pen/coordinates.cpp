#include <stdio.h>
#include <opencv2/opencv.hpp>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>
#include "pixy.h"


using namespace cv;
using namespace std;

#define Computer 0

#define HEIGHT 22
#define WIDTH 13.4
#define UPPER_LEFT 0, 0
#define UPPER_RIGHT 13.4, 0
#define LOWER_LEFT 0, 22
#define LOWER_RIGHT 13.4, 22

#define PIXY_X_RESOLUTION 320
#define PIXY_Y_RESOLUTION 200
#define CENTER_X PIXY_X_RESOLUTION/2 
#define CENTER_Y PIXY_Y_RESOLUTION/2

// --------------------------------------------------------------------
//                            PIXY variables                         //
// --------------------------------------------------------------------
#define BLOCK_BUFFER_SIZE    25
// Pixy Block buffer // 
struct Block blocks[BLOCK_BUFFER_SIZE];
static bool run_flag = true;
static Mat hmat         = Mat::zeros( Size(3, 3), CV_8UC3 );
// --------------------------------------------------------------------
//                          Helper Functions                         //
// --------------------------------------------------------------------
void CallBackFunc(int event, int x, int y, int flags, void* userdata);
void MyFilledCircle( Mat img, Point center );
void handle_SIGINT(int unused);

// --------------------------------------------------------------------
//                                 Main                              //
// --------------------------------------------------------------------
int main(int argc, char** argv )
{
    /* ************************************************* **
    **                     Setup Pixy                    **
    ** 1.Handle ctrl+c signal                            **
    ** 2.Connect to Pixy camera by initializing it       **
    ** 3.Request and read Pixy firmware version          **
    ** ************************************************* */

    // Catch CTRL+C (SIGINT) signals //
    signal(SIGINT, handle_SIGINT);

    int      i = 0;
    int      index;
    int      blocks_copied;
    int      pixy_init_status;
    char     buf[128];

    printf("Hello Pixy:\n libpixyusb Version: %s\n", __LIBPIXY_VERSION__);

    // Connect to Pixy //
    pixy_init_status = pixy_init();

    // Was there an error initializing pixy? //
    if(!(pixy_init_status == 0)){
        // Error initializing Pixy //
        printf("pixy_init(): ");
        pixy_error(pixy_init_status);

        return pixy_init_status;
    }

    // Request Pixy firmware version //
    {
        uint16_t major;
        uint16_t minor;
        uint16_t build;
        int      return_value;

        return_value = pixy_get_firmware_version(&major, &minor, &build);

        if (return_value){
          // Error //
          printf("Failed to retrieve Pixy firmware version. ");
          pixy_error(return_value);

          return return_value;
        } 
        else{
          // Success //
          printf(" Pixy Firmware Version: %d.%d.%d\n", major, minor, build);
        }
    }
#if Computer
    /* ************************************************* **
    **                  Calibration Window               **
    ** Create a full-creen window and mark the 4 corners **
    ** ************************************************* */
    // Create image 
    Mat img = Mat::zeros( Size(WIDTH, HEIGHT), CV_8UC3 );
    MyFilledCircle( img, Point(UPPER_LEFT));
    MyFilledCircle( img, Point(UPPER_RIGHT));
    MyFilledCircle( img, Point(LOWER_LEFT));
    MyFilledCircle( img, Point(LOWER_RIGHT));


    //Create a window
    string win_title = "win";
    namedWindow(win_title, CV_WINDOW_NORMAL);

    //set the callback function for any mouse event
    setMouseCallback(win_title, CallBackFunc, NULL);
    imshow(win_title, img);
    
#endif
    /* ************************************************* **
    **          Calibrate coordinates from pixy          **
    ** Requires minimum 4 points (4 corners)             **
    ** Uses "findHomography" function to find            ** 
    ** homography matrix "hmat"                          **
    ** ************************************************* */
    vector<Point2f> pts_dst;
    pts_dst.push_back(Point2f(UPPER_LEFT));
    pts_dst.push_back(Point2f(UPPER_RIGHT));
    pts_dst.push_back(Point2f(LOWER_RIGHT));
    pts_dst.push_back(Point2f(LOWER_LEFT));

    // Four corners of the book in source image
    vector<Point2f> pts_src;


    printf("Calibration starts...\n");
    int detected_points = 0;
    bool corners[4] = {false, false, false, false};
    while(run_flag)
    {
    // Wait for new blocks to be available //
        while(!pixy_blocks_are_new() && run_flag); 

        // Get blocks from Pixy //
        blocks_copied = pixy_get_blocks(BLOCK_BUFFER_SIZE, &blocks[0]);

        if(blocks_copied < 0){
            // Error: pixy_get_blocks //
            printf("pixy_get_blocks(): ");
            pixy_error(blocks_copied);
        }

        // Display received blocks //
        //printf("frame %d:\n", i);
        // TODO: Make sure only detecting one signature!
        // TODO: take average of multiple readings
        // Dont need the for loop
        // Manually setting index to one! 
        for(index = 0; index != blocks_copied; ++index) {    
           if(blocks[index].x && blocks[index].y){
                if(!corners[0] && (blocks[index].x - CENTER_X < 0) && (blocks[index].y - CENTER_Y < 0)){
                    corners[0] = true;
                    pts_src.push_back(Point2f(blocks[index].x, blocks[index].y)); 
                    detected_points++;
                    printf("*** Point Collected! ***\n");
                }
                else if(!corners[1] && corners[0] && (blocks[index].x - CENTER_X > 0) && (blocks[index].y - CENTER_Y < 0)){
                    corners[1] = true;
                    pts_src.push_back(Point2f(blocks[index].x, blocks[index].y)); 
                    detected_points++;
                    printf("*** Point Collected! ***\n");
                }
                else if(!corners[2] && corners[0] && corners[1] && (blocks[index].x - CENTER_X > 0) && (blocks[index].y - CENTER_Y > 0)){
                    corners[2] = true;
                    pts_src.push_back(Point2f(blocks[index].x, blocks[index].y)); 
                    detected_points++;
                    printf("*** Point Collected! ***\n");
                }
                else if(!corners[3] && corners[0] && corners[1] && corners[2] && (blocks[index].x - CENTER_X < 0) && (blocks[index].y - CENTER_Y > 0)){
                    corners[3] = true;
                    pts_src.push_back(Point2f(blocks[index].x, blocks[index].y)); 
                    detected_points++;
                    printf("*** Point Collected! ***\n");
                }    
            }
            if(detected_points == 4){
                run_flag = false;
            }
        }
        i++;
    }

    for(int j = 0; j < pts_src.size(); j++)
        printf("Points: x = %f, y = %f \n", pts_src.at(j).x, pts_src.at(j).y);

    // Calculate Homography
    hmat = findHomography(pts_src, pts_dst);

    /* ************************************************* **
    **    Read and transform coordinates from pixy       **
    ** Reads coordinates from pixy and tranforms them    **
    ** using the perspectiveTransform function.          ** 
    ** ************************************************* */
    vector<Point2f> pixyPoints;
    vector<Point2f> finalPoints;

    run_flag = true; i = 0;
    printf("Detecting blocks...\n");
    while(run_flag)
    {
        // Wait for new blocks to be available //
        while(!pixy_blocks_are_new() && run_flag); 

        // Get blocks from Pixy //
        blocks_copied = pixy_get_blocks(BLOCK_BUFFER_SIZE, &blocks[0]);

        if(blocks_copied < 0) {
          // Error: pixy_get_blocks //
          printf("pixy_get_blocks(): ");
          pixy_error(blocks_copied);
        }

        // Display received blocks //
        for(index = 0; index != blocks_copied; ++index) {   
            if(blocks[index].x && blocks[index].y){ 
               pixyPoints.push_back(Point2f(blocks[index].x, blocks[index].y));
               printf("\nframe %d:\n", i);
               perspectiveTransform(pixyPoints, finalPoints, hmat);
               if(finalPoints.size() != 0){
                   printf("x: %f \n", finalPoints[finalPoints.size() - 1].x);
                   printf("y: %f \n\n", finalPoints[finalPoints.size() - 1].y);
               }
               pixyPoints.pop_back();
            }
            i++;
        }
    }
    pixy_close();
    
    // Wait until user press some key
    //waitKey(0);

    return 0;

}


void CallBackFunc(int event, int x, int y, int flags, void* userdata)
{
     if  ( event == EVENT_LBUTTONDOWN )
     {
          cout << "Left button of the mouse is clicked - position (" << x << ", " << y << ")" << endl;
     }
     else if  ( event == EVENT_RBUTTONDOWN )
     {
          cout << "Right button of the mouse is clicked - position (" << x << ", " << y << ")" << endl;
     }
     else if  ( event == EVENT_MBUTTONDOWN )
     {
          cout << "Middle button of the mouse is clicked - position (" << x << ", " << y << ")" << endl;
     }
     else if ( event == EVENT_MOUSEMOVE )
     {
          cout << "Mouse move over the window - position (" << x << ", " << y << ")" << endl;

     }
}
void MyFilledCircle( Mat img, Point center )
{
 int thickness = -1;
 int lineType = 8;

 circle( img,
         center,
         5,
         Scalar( 0, 0, 255 ),
         thickness,
         lineType );
}
void handle_SIGINT(int unused)
{
  // On CTRL+C - abort! //

  run_flag = false;
}