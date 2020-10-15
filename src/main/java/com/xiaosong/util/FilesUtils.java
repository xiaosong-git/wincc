package com.xiaosong.util;

import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class FilesUtils {
	/**
	 * 生成Byte流 TODO
	 * 
	 * @history @knownBugs @param @return @exception
	 */
	public static byte[] getBytesFromFile(File file) {
		byte[] ret = null;
		try {
			if (file == null) {
				// log.error("helper:the file is null!");
				return null;
			}
			FileInputStream in = new FileInputStream(file);
			ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
			byte[] b = new byte[4096];
			int n;
			while ((n = in.read(b)) != -1) {
				out.write(b, 0, n);
			}
			in.close();
			out.close();
			ret = out.toByteArray();
		} catch (IOException e) {
			// log.error("helper:get bytes from file process error!");
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * 把流生成图片 TODO
	 * 
	 * @history @knownBugs @param @return @exception
	 */
	public static File getFileFromBytes(byte[] files, String outputFile, String fileName) {
		File ret = null;

		BufferedOutputStream stream = null;
		try {
			if (StringUtils.isBlank(fileName)) {
				ret = new File(outputFile);
			} else {
				ret = new File(outputFile + fileName);
			}
			File fileParent = ret.getParentFile();
		
			
			if (!fileParent.exists()) {
				fileParent.mkdirs();
			}
			ret.createNewFile();

			FileOutputStream fstream = new FileOutputStream(ret);

			stream = new BufferedOutputStream(fstream);

			stream.write(files);

		} catch (Exception e) {

			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// log.error("helper:get file from byte process error!");
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	/***
	 * 根据路径获取
	 * 
	 * @param path
	 * @return
	 */
	public static byte[] getPhoto(String path) {
		byte[] data = null;
		FileImageInputStream input = null;
		try {
			input = new FileImageInputStream(new File(path));
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int numBytesRead = 0;
			while ((numBytesRead = input.read(buf)) != -1) {
				output.write(buf, 0, numBytesRead);
			}
			data = output.toByteArray();
			output.close();
			input.close();
		} catch (FileNotFoundException ex1) {
			ex1.printStackTrace();
		} catch (IOException ex1) {
			ex1.printStackTrace();
		}
		return data;
	}

	/**
	 * 将图片压缩
	 * @param srcImgData
	 * @param maxSize  10240L （10KB）
	 * @return
	 * @throws Exception
	 */

	public static byte[] compressUnderSize(byte[] srcImgData, long maxSize)
			throws Exception {
		double scale = 0.9;
		byte[] imgData = Arrays.copyOf(srcImgData, srcImgData.length);

		if (imgData.length > maxSize) {
			do {
				try {
					imgData = compress(imgData, scale);

				} catch (IOException e) {
					return null;

				}

			} while (imgData.length > maxSize);
		}

		return imgData;
	}

	public static byte[] compress(byte[] srcImgData, double scale) throws IOException {
		BufferedImage bi = ImageIO.read(new ByteArrayInputStream(srcImgData));
		int width = (int) (bi.getWidth() * scale); // 源图宽度
		int height = (int) (bi.getHeight() * scale); // 源图高度

		Image image = bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		BufferedImage tag = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);

		Graphics g = tag.getGraphics();
		g.setColor(Color.RED);
		g.drawImage(image, 0, 0, null); // 绘制处理后的图
		g.dispose();

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		ImageIO.write(tag, "JPEG", bOut);

		return bOut.toByteArray();
	}
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws Exception {
//	byte []aBytes=	getPhoto("E://测试.jpg");
		try {
			File test = new File("E:\\WEB-INF\\13601313885.jpg");
			String name = test.getName();
			String absolutePath = test.getAbsolutePath();
			String path = test.getParentFile().getPath();

			String b=Base64_2.encode(FilesUtils.compressUnderSize(getPhoto(absolutePath), 1024*90L));
			byte[] photoKey = Base64_2.decode(b);
			FilesUtils.getFileFromBytes(photoKey, path, name);


//			System.out.println(System.currentTimeMillis());
//
//			Date time = new Date(1994362053814L);
//			SimpleDateFormat formats = new SimpleDateFormat("yyyy年MM月dd日  hh:mm:ss");
//			System.out.println(formats.format(time));
//
//			String date = "2020-06-18 10:46";
//			String date1 = "2020-06-18 11:16";
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");//要转换的日期格式，根据实际调整""里面内容
//			try {
//				long dateToSecond = sdf.parse(date).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
//				long dateToSecond1 = sdf.parse(date1).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
//				System.out.print(dateToSecond);
//				System.out.print(dateToSecond1);
//			}catch (Exception e){
//				e.printStackTrace();
//			}
			/*if (a.equals(b)) {
				System.out.println("===");
			}else {
				System.out.println("!=");
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}


	}


	/**
	 * 获取网络地址图片
	 * @param strUrl
	 * @return
	 */
	public static byte[] getImageFromNetByUrl(String strUrl) {
		try {
			URL url = new URL(strUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5 * 1000);
			InputStream inStream = conn.getInputStream();// 通过输入流获取图片数据
			byte[] btImg = readInputStream(inStream);// 得到图片的二进制数据
			return btImg;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 从输入流中获取数据
	 *
	 * @param inStream
	 *            输入流
	 * @return
	 * @throws Exception
	 */
	public static byte[] readInputStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[10240];
		int len = 0;
		while ((len = inStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, len);
		}
		inStream.close();
		return outStream.toByteArray();
	}
}
