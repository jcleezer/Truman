package com.leezer.truman;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.harium.storage.kdtree.KDTree;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Trumanizer {

    private final File baseImageFile;
    private final Map<String,RGB> images;
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
                System.out.println("Working on "+x+","+y);
                BufferedImage subImage = base.getSubimage(x*tileWidth,y*tileHeight,tileWidth,tileHeight);
                RGB rgb = getRGB(subImage);
                KDTree<File> index = buildKDTree();
                final double[] key = new double[]{rgb.getRed(),rgb.getGreen(),rgb.getBlue()};
                final File nearestNeighbor = index.nearest(key);
                System.out.println("Found "+nearestNeighbor.getName()+" for "+x+", "+y);
                removeImage(nearestNeighbor);
                final BufferedImage shrunkImage = resizeImage(ImageIO.read(nearestNeighbor),tileWidth,tileHeight);
                // Write image to output
                System.out.println("Writing to output");
                for (int w = 0; w < shrunkImage.getWidth(); w++){
                    for (int h = 0; h < shrunkImage.getHeight(); h++){
                        output.setRGB((x*tileWidth)+w,(y*tileHeight)+h,shrunkImage.getRGB(w,h));
                    }
                }
            }
        }
        return output;
    }

    private KDTree<File> buildKDTree(){
        final KDTree<File> index = new KDTree<File>(3);
        for (Map.Entry<String,RGB> entries : images.entrySet()){
            RGB rgb = entries.getValue();
            try {
                //insert throws an exception when key exists
                index.insert(new double[]{rgb.getRed(), rgb.getGreen(), rgb.getBlue()}, new File(entries.getKey()));
            } catch (RuntimeException e){
                //e.printStackTrace();
            }
        }
        return index;
    }
    private void removeImage(File key) {
//        try {
//            images.delete(key,true);
//        }catch (RuntimeException e){
//            e.printStackTrace();
//        }
        images.remove(key.getAbsolutePath());
    }


    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height){
        System.out.println("Resizing");
        BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width,height, null);
        g.dispose();

        return resizedImage;
    }

    private Map<String,RGB> buildImageIndex(File imageDir) throws IOException {
        final Gson gson = new Gson();
        //final KDTree<File> index = new KDTree<>(3);
        final Map<String,RGB> values = new HashMap<>();
        final File persisted = new File(imageDir,"truman.json");
        if (persisted.exists()){
            Type typeOfHashMap = new TypeToken<Map<String, RGB>>() { }.getType();
            Map<String,RGB> persistedRgbs = gson.fromJson(new FileReader(persisted),typeOfHashMap);
            if (persistedRgbs != null) {
                values.putAll(persistedRgbs);
            }
        }

        Files.walk(Paths.get(imageDir.getAbsolutePath()))
                .filter(Files::isRegularFile)
                .filter(f ->isValidPhotoType(f.toString()))
                .forEach( path -> {if (!values.containsKey(path.toString()))addToMap(values,path);});

        //Persist
        FileUtils.writeStringToFile(persisted, gson.toJson(values));
        //IOUtils.write(gson.toJson(values),new FileWriter(persisted));
        return values;
    }

    private boolean isValidPhotoType(String name){
        final String lower = name.toLowerCase();
        return lower.endsWith("jpg") || lower.endsWith("jpeg");
    }

    private void addToMap(Map<String,RGB> values, Path path)  {
        final File imageFile = path.toFile();

        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image != null) {
                RGB rgb = getRGB(image);

                System.out.println("Processing " + path.toFile().getName());

//            System.out.println("Red Color value = "+ rgb.getRed());
//            System.out.println("Green Color value = "+ rgb.getGreen());
//            System.out.println("Blue Color value = "+ rgb.getBlue());

                values.put(path.toString(), rgb);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private RGB getRGB(BufferedImage image){
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
        return new RGB(red,green,blue);
    }
}
