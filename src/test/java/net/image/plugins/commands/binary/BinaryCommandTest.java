/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2016 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.image.plugins.commands.binary;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Dilate;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Erode;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.type.logic.BitType;

/**
 *
 * @author cyril
 */
public class BinaryCommandTest {

    public static void testCircleGeneration() {

        Img<BitType> emptyCircle = generateEmptyCircle(20,2);

        System.out.println(countDarkPixel(emptyCircle));
        print(emptyCircle);
    }

    public static void main(String... args) {

        testCircleGeneration();
        testFillCommand();
    }

    
    public static Img<BitType> generateEmptyImage(int width) {
        final ImgFactory< BitType> imgFactory = new ArrayImgFactory<BitType>();
        return imgFactory.create(new long[]{width,width}, new BitType());
    }
    
    /**
     * Generate an image containing an empty circle
     *
     * @return
     */
    public static Img<BitType> generateEmptyCircle(int width,int thinkness) {

        Img<BitType> img = generateEmptyImage(width);
        int r = width/2;
        
        for(int i = 0; i!=thinkness;i++) {
        drawCircle(img,r-(i+1), r,r);
        
        }
        
        return img;

    }
    public static void drawCircle(Img<BitType> img, int r, int x0, int y0) {
        
        
        
        int x = r;
        
        int y = 0;
        int err = 0;

        RandomAccess<BitType> randomAccess = img.randomAccess();

        while (x >= y) {
            putpixel(randomAccess, x0 + x, y0 + y);
            putpixel(randomAccess, x0 + y, y0 + x);
            putpixel(randomAccess, x0 - y, y0 + x);
            putpixel(randomAccess, x0 - x, y0 + y);
            putpixel(randomAccess, x0 - x, y0 - y);
            putpixel(randomAccess, x0 - y, y0 - x);
            putpixel(randomAccess, x0 + y, y0 - x);
            putpixel(randomAccess, x0 + x, y0 - y);

            y += 1;
            err += 1 + 2 * y;
            if (2 * (err - x) + 1 > 0) {
                x -= 1;
                err += 1 - 2 * x;
            }
        }

      

    }

    protected static void putpixel(RandomAccess<BitType> r, int x, int y) {
        r.setPosition(new long[]{x, y});
        r.get().set(true);
    }

    protected static int countDarkPixel(Img<BitType> img) {
        Cursor<BitType> cursor = img.cursor();
        cursor.reset();
        int count = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            if (cursor.get().get()) {
                count++;
            }
        }

        return count;
    }

    public static void print(Img<BitType> r) {
        long width = r.dimension(0);
        long height = r.dimension(1);
        
        RandomAccess<BitType> randomAccess = r.randomAccess();
        StringBuilder builder = new StringBuilder((int)(width*height));
        for (long y = 0; y != height; y++) {
            for (long x = 0; x != width; x++) {
                randomAccess.setPosition(new long[]{x,y});
                if(randomAccess.get().get()) {
                    builder.append("o");
                }
                else {
                    builder.append("-");
                }
            }
            builder.append("\r\n");
        }
        
        System.out.println(builder.toString());

    }

    public static void testBinaryOps(String title, UnaryOperation op) {
        Img<BitType> circle = generateEmptyCircle(20,3);
        Img<BitType> result = generateEmptyImage(20);
        System.out.println(String.format("############# %s #############",title));
        System.out.println("Before");
        print(circle);
        op.compute(circle,result);
        System.out.println("After");
        print(result);
    }
    
    public static void testFillCommand() {
        
        Dilate dilate = new Dilate(ConnectedType.FOUR_CONNECTED, new OutOfBoundsConstantValueFactory<BitType, RandomAccessibleInterval<BitType>>(new BitType(false)), 1);
        Erode erode = new Erode(ConnectedType.FOUR_CONNECTED, new OutOfBoundsConstantValueFactory<BitType, RandomAccessibleInterval<BitType>>(new BitType(false)), 1);
        testBinaryOps("Dilate",dilate);
        testBinaryOps("Erode",erode);
        /*
        Img<BitType> circle = generateEmptyCircle(20,2);
        Img<BitType> result = generateEmptyImage(20);
        
        //print(result);
        System.out.println("Circle : ");
        print(circle);
        System.out.println("Result : ");
        
        Dilate erode = new Dilate(ConnectedType.FOUR_CONNECTED, new OutOfBoundsConstantValueFactory<BitType, RandomAccessibleInterval<BitType>>(new BitType(false)), 1);
        erode.compute(circle, result);
        print(result);*/
    }
    
}
