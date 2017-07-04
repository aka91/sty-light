#!/usr/bin/env python

"""
 /*
 * Copyright (C) 2016 Jones Chi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"""
# to run:
# cd receiver
# $ adb forward tcp:53516 tcp:53515
# $ python cs_receiver_conn.py
#
from subprocess import Popen, PIPE, STDOUT
import socket
import cv2
import pickle
import numpy
import struct ## new
from sys import getsizeof

PORT = 53516
bufferSize = 1024

SAVE_TO_FILE = True
def connect_to_server():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('localhost', PORT)
    print 'Connecting to %s port %s' % server_address
    sock.connect(server_address)

    cv2.startWindowThread()
    cv2.namedWindow("frame")
    try:
        # Send data
        message = 'mirror\n'
        print 'Sending mirror cmd'
        sock.sendall(message)

	if SAVE_TO_FILE:
            f = open('video_client.raw', 'wb')
        p = Popen(['ffplay', '-framerate', '30', '-'], stdin=PIPE, stdout=PIPE)
        #p = Popen(['gst-launch-1.0', 'fdsrc', '!', 'h264parse', '!', 'avdec_h264', '!', 'autovideosink'], stdin=PIPE, stdout=PIPE)
        skiped_metadata = False
        while True:
            data = sock.recv(bufferSize)
            if data == None or len(data) <= 0:
                print 'No data\n'
                break
            #print 'receiving data\n'
            if not skiped_metadata:
                if data.find('\r\n\r\n') > 0:
                    last_ctrl = data.find('\r\n\r\n') + 4
                    print 'Recv control data: ', data[0:last_ctrl]
                    print 'Size of data: ', getsizeof(data)
                    if len(data) > last_ctrl:
                        p.stdin.write(data[last_ctrl:])
	                if SAVE_TO_FILE:
                            f.write(data[last_ctrl:])
                skiped_metadata = True
            else:
                p.stdin.write(data)
                #frame = numpy.fromstring(data, dtype=numpy.uint8)
                #frame = numpy.reshape(frame, (1280,720,3))
                #frame = pickle.loads(data)
                #print frame
                #cv2.imshow('frame',frame)
                #cv2.waitKey(0)
	        if SAVE_TO_FILE:
                    f.write(data)
        p.kill()
	if SAVE_TO_FILE:
            f.close()

    finally:
        sock.close()

if __name__ == "__main__":
    connect_to_server()
