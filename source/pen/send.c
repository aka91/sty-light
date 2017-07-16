#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <errno.h>

int main(int argc, char** argv) {
 system("sudo systemctl stop serial-getty@ttyS0.service"); //might be ttyAMA0 instead of ttyS0
 int sfd = open("/dev/serial0", O_RDWR | O_NOCTTY); //Might be serial1
 if (sfd == -1) {
  printf("Error no is : %d\n", errno);
  printf("Error description is : %s\n", strerror(errno));
  return (-1);
 };
 struct termios options;
 tcgetattr(sfd, &options);
 cfsetspeed(&options, B9600);
 cfmakeraw(&options);
 options.c_cflag &= ~CSTOPB;
 options.c_cflag |= CLOCAL;
 options.c_cflag |= CREAD;
 options.c_cc[VTIME]=0;
 options.c_cc[VMIN]=0;
 tcsetattr(sfd, TCSANOW, &options);
 
 //int data1 = 234;
 //int data2 = 546;
 //char char_data1 = char (data1);
 //char char_data2 = char (data2);
 //char buf[] = char_data1 + char_data2;
 char buf[] = "hello world, sdjfasd,fbsk.fdjbaks.bjsfkd";
 char buf2[100];
 char c;
 int count = write(sfd, buf,strlen(buf)+1);
 
 /*
 int i=0;
 while(1){
  count = read(sfd, &c, 1);
  //printf("Enters while loop, count is : %d\n", count);
  if(count!=0){
    buf2[i]=c;
    //printf("Count is not 0\n");
    i++;
    if(c==0){
		//printf("Count is 0\n");
		break;
	};
  };
 
 };
 */
 //printf("%s\n\r", buf2);
 close(sfd);
 return (EXIT_SUCCESS);
}
