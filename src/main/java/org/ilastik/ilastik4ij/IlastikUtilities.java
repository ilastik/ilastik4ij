/**
 * MIT License
 * 
 * Copyright (c) 2017 ilastik
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * Author: Carsten Haubold
 */

package org.ilastik.ilastik4ij;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.scijava.log.LogService;

public class IlastikUtilities {
	/*
	 * Utility method to obtain a unique filename
	 * @param extension
	 * @return
	 * @throws IOException
	 */
	public static String getTemporaryFileName(String extension) throws IOException
	{
		File f = File.createTempFile("ilastik4j", extension, null);
		String filename = f.getAbsolutePath();
		f.delete();
		return filename;
	}
	
    /*
     * Redirect an input stream to the log service (used for command line output)
     *
     * @param in input stream
     * @param logger
     * @throws IOException
     */
    public static void redirectOutputToLogService(final InputStream in, final LogService logger, final Boolean isErrorStream) {
        Thread t = new Thread() {
            @Override
            public void run() {

                String line;

                try (BufferedReader bis = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()))) {
                    while ((line = bis.readLine()) != null) {
                        if (isErrorStream) {
                            logger.error(line);
                        } else {
                            logger.info(line);
                        }
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException("Could not read ilastik output", ioe);
                }
            }
        };

//        t.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(KNIPGateway.log()));
        t.start();
    }
}
