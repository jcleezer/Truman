package com.leezer.truman;

import com.harium.storage.kdtree.KDTree;
import com.harium.storage.kdtree.exception.KeyDuplicateException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Trumanizer {

    private final File baseImageFile;
    private final KDTree<File> images;
    private final int gridWidth;
    private final int gridHeight;


    public Trumanizer(File baseImage, File imageDir, int width, int height) throws IOException {
        this.baseImageFile = baseImage;
        this.images = buildImageIndex(imageDir);
        this.gridHeight = height;
        this.gridWidth = width;
    }

    public BufferedImage trumanize() throws IOException {
        final BufferedImage base = ImageIO.read(baseImageFile);
        int tileWidth = Math.round(base.getWidth()/gridWidth);
        int tileHeight = Math.round(base.getHeight()/gridHeight);
        final BufferedImage output = new BufferedImage(tileWidth*gridWidth,tileHeight*gridHeight,base.getType());



        //TODO Change this to be random order...
        for (int x = 0; x < gridWidth; x++){
            for (int y = 0; y < gridHeight; y++){
                BufferedImage subImage = base.getSubimage(x*tileWidth,y*tileHeight,tileWidth,tileHeight);
                double[] rgb = getRGB(subImage);
                final File nearestNeighbor = images.nearest(rgb);
                final BufferedImage shrunkImage = resizeImage(ImageIO.read(nearestNeighbor),tileWidth,tileHeight);
                // Write image to output
                for (int w = 0; w < shrunkImage.getWidth(); w++){
                    for (int h = 0; h < shrunkImage.getHeight(); h++){
                        output.setRGB((x*tileWidth)+w,(y*tileHeight)+h,shrunkImage.getRGB(w,h));
                    }
                }
            }
        }
        return output;
    }


    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height){
        BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width,height, null);
        g.dispose();

        return resizedImage;
    }

    private KDTree<File> buildImageIndex(File imageDir) throws IOException {
        final KDTree<File> index = new KDTree<>(3);
        Files.walk(Paths.get(imageDir.getAbsolutePath()))
                .filter(Files::isRegularFile)
                .filter(f -> f.toString().toLowerCase().endsWith("jpg"))
                .forEach( path -> addToTree(index,path));
        System.out.println("Built index of "+index.size()+" images");
        return index;
    }

    private void addToTree(KDTree<File> index, Path path)  {
        final File imageFile = path.toFile();

        try {
            BufferedImage image = ImageIO.read(imageFile);
            double[] rgb = getRGB(image);

            System.out.println("Processing "+path.toFile().getName());
            System.out.println("Red Color value = "+ rgb[0]);
            System.out.println("Green Color value = "+ rgb[1]);
            System.out.println("Blue Color value = "+ rgb[2]);

            index.insert(rgb,imageFile);
        } catch (IOException | KeyDuplicateException e) {
            e.printStackTrace();
        }

    }

    public double[] getRGB(BufferedImage image){
        int count = 0;

        int redTotal = 0;
        int blueTotal = 0;
        int greenTotal = 0;

        for (int h = 0; h < image.getHeight(); h++){
            for (int w = 0; w < image.getWidth(); w++){
                // Getting pixel color by position x and y
                int clr=  image.getRGB(w,h);
                int  red   = (clr & 0x00ff0000) >> 16;
                int  green = (clr & 0x0000ff00) >> 8;
                int  blue  =  clr & 0x000000ff;
                redTotal+=red;
                greenTotal+=green;
                blueTotal+=blue;

                count++;
            }
        }
        double blue = blueTotal/count;
        double red = redTotal/count;
        double green = greenTotal/count;
        return new double[]{red,green,blue};
    }
}
