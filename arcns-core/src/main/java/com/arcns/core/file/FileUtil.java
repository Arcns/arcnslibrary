package com.arcns.core.file;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

    public static boolean isEmptyOrBlank(String str) {
        if (str == null || str.trim().length() == 0)
            return true;
        else
            return false;
    }

    /**
     * 检查文件是否为某个后缀名（不区分大小写）
     */
    public static Boolean checkFileSuffix(String filePath, String... suffixs) {
        if (filePath == null) return false;
        for (String suffix : suffixs) {
            if (filePath.toLowerCase().endsWith(suffix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件后缀名(带.)
     */
    public static String getFileSuffix(String filePath) {
        if (filePath == null) return null;
        if (filePath.contains("."))
            return filePath.substring(filePath.lastIndexOf("."));
        else
            return "";
    }

    /**
     * 获取文件的目录部分（结尾带/）
     */
    public static String getFileDirectory(String filePath) {
        if (filePath.contains(File.separator))
            return filePath.substring(0, filePath.lastIndexOf(File.separator));
        else
            return "";
    }

    /**
     * 获取文件名（不带后缀名）
     */
    public static String getFileNameNotSuffix(String filePath) {
        filePath = getFileName(filePath);
        if (filePath.contains(".")) {
            return filePath.substring(0, filePath.lastIndexOf('.'));
        } else {
            return filePath;
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String filePath) {
        if (filePath.contains(File.separator))
            return filePath.substring(filePath.lastIndexOf(File.separator) + 1);
        else
            return filePath;
    }

    public static String splicing(String dirPath, String fileName) {
        return getSupplementaryDirPath(dirPath) + fileName;
    }

    /**
     * 补全文件夹路径（确保最后字符是/）
     */
    public static String getSupplementaryDirPath(String dirPath) {
        if (!dirPath.endsWith(File.separator))
            return dirPath += File.separator;
        else
            return dirPath;
    }

    /**
     * 删除文件或目录（包含目录下文件）
     */
    public static void removeFile(String filePath) {
        if (filePath == null || filePath.length() == 0) {
            return;
        }
        File file = new File(filePath);
        if (file.exists()) {
            removeFile(file);
        }
    }

    /**
     * 删除文件或目录（包含目录下文件）
     */
    public static void removeFile(File file) {
        //如果是文件直接删除
        if (file.isFile()) {
            file.delete();
            return;
        }
        //如果是目录，递归判断，如果是空目录，直接删除，如果是文件，遍历删除
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }
            for (File childFile : childFiles) {
                removeFile(childFile);
            }
            file.delete();
        }
    }

    /**
     * 创建目录，如果不存在的话
     */
    public static void mkdirIfNotExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 读取内容
     */
    public static String readerContent(String fromFilePath) {
        if (isEmptyOrBlank(fromFilePath)) {
            return null;
        }
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(fromFilePath));
            StringBuffer content = new StringBuffer();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 写入内容
     */
    public static String writerContent(String toFileDir, String toFileName, WriterProcess<BufferedWriter> writerProcess) {
        if (isEmptyOrBlank(toFileDir) || isEmptyOrBlank(toFileName) || writerProcess == null) {
            return null;
        }
        // 判断目标目录是否存在
        mkdirIfNotExists(toFileDir);
        // 创建目标文件
        toFileDir = getSupplementaryDirPath(toFileDir);
        String toFilePath = toFileDir + toFileName;
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(toFilePath));
            writerProcess.onWriter(bufferedWriter);
            return toFilePath;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 写入内容
     */
    public static Boolean writerContent(Context context, byte[] content, Uri toFileUri) {
        return writerContent(context, toFileUri, writer -> writer.write(content));
    }

    /**
     * 写入内容
     */
    public static Boolean writerContent(Context context, Uri toFileUri, WriterProcess<FileOutputStream> writerProcess) {
        if (toFileUri == null || writerProcess == null) {
            return null;
        }
        ParcelFileDescriptor parcelFileDescriptor = null;
        FileOutputStream fileOutputStream = null;
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(toFileUri, "w");
            fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
            writerProcess.onWriter(fileOutputStream);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 写入内容
     */
    public static String writerContent(String content, String toFileDir, String toFileName) {
        return writerContent(toFileDir, toFileName, writer -> writer.write(content));
    }

    /**
     * 复制文件
     */
    public static String copyFile(String fromFilePath, String toFileDir) {
        return copyFile(fromFilePath, toFileDir, null);
    }

    /**
     * 复制文件
     */
    public static String copyFile(String fromFilePath, String toFileDir, String toFileName) {
        if (isEmptyOrBlank(fromFilePath)) {
            return null;
        }
        if (isEmptyOrBlank(toFileDir) && isEmptyOrBlank(toFileName)) {
            return null;
        }
        File fromFile = new File(fromFilePath);
        if (!fromFile.exists()) {
            return null;
        }
        // 默认目标目录为原目录
        if (isEmptyOrBlank(toFileDir)) {
            toFileDir = getFileDirectory(fromFilePath);
        }
        // 默认新文件名为原文件名
        if (isEmptyOrBlank(toFileName)) {
            toFileName = fromFile.getName();
        }
        // 判断目标目录是否存在
        mkdirIfNotExists(toFileDir);
        // 创建目标文件
        toFileDir = getSupplementaryDirPath(toFileDir);
        File toFile = new File(toFileDir + toFileName);
        // 开始复制
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(fromFile);
            outputStream = new FileOutputStream(toFile);
            int byteCount = 0;
            byte[] bytes = new byte[1024];
            while ((byteCount = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, byteCount);
            }
            return toFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 复制文件
     */
    public static String copyFile(InputStream inputStream, String toFileDir, String toFileName) {
        return copyFile(inputStream, toFileDir, toFileName, null);
    }

    /**
     * 复制文件
     */
    public static String copyFile(InputStream inputStream, String toFileDir, String toFileName, ProgressListener progressListener) {
        if (inputStream == null) {
            return null;
        }
        if (isEmptyOrBlank(toFileDir) || isEmptyOrBlank(toFileName)) {
            return null;
        }
        // 判断目标目录是否存在
        mkdirIfNotExists(toFileDir);
        // 创建目标文件
        toFileDir = getSupplementaryDirPath(toFileDir);
        File toFile = new File(toFileDir + toFileName);
        // 开始复制
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(toFile);
            double totalCount = -1;
            try {
                totalCount = ((FileInputStream) inputStream).getChannel().size();
            } catch (Exception e) {

            }
            double cumulativeCount = 0;
            int byteCount = 0;
            byte[] bytes = new byte[1024];
            while ((byteCount = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, byteCount);
                if (progressListener != null && totalCount != -1) {
                    cumulativeCount += byteCount;
                    int progress = (int) (cumulativeCount / totalCount * 100);
                    progressListener.update(progress);
                }
            }
            return toFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 复制文件
     */
    public static boolean copyFile(String fromFilePath, OutputStream outputStream) {
        return copyFile(fromFilePath, outputStream, null);
    }

    /**
     * 复制文件
     */
    public static boolean copyFile(String fromFilePath, OutputStream outputStream, ProgressListener progressListener) {
        if (isEmptyOrBlank(fromFilePath)) {
            return false;
        }
        if (outputStream == null) {
            return false;
        }
        File fromFile = new File(fromFilePath);
        if (!fromFile.exists()) {
            return false;
        }
        // 开始复制
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fromFile);
            double totalCount = fromFile.length();
            double cumulativeCount = 0;
            int byteCount = 0;
            byte[] bytes = new byte[1024];
            while ((byteCount = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, byteCount);
                if (progressListener != null) {
                    cumulativeCount += byteCount;
                    int progress = (int) (cumulativeCount / totalCount * 100);
                    progressListener.update(progress);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 复制文件夹
     */
    public static void copyDir(String fromDirPath, String toDirPath) {
        if (isEmptyOrBlank(fromDirPath) || isEmptyOrBlank(toDirPath)) {
            return;
        }
        File fromDir = new File(fromDirPath);
        if (!fromDir.exists() && !fromDir.isDirectory()) {
            return;
        }
        File[] childFiles = fromDir.listFiles();
        if (childFiles == null || childFiles.length == 0) {
            return;
        }
        mkdirIfNotExists(toDirPath);
        toDirPath = getSupplementaryDirPath(toDirPath);
        for (File childFile : childFiles) {
            if (childFile.isDirectory()) {
                copyDir(childFile.getAbsolutePath(), toDirPath + childFile.getName());
            } else {
                copyFile(childFile.getAbsolutePath(), toDirPath);
            }
        }
    }

    /**
     * 剪切文件
     */
    public static String moveFile(String fromFilePath, String toFileDir) {
        return moveFile(fromFilePath, toFileDir, null);
    }

    /**
     * 剪切文件
     */
    public static String moveFile(String fromFilePath, String toFileDir, String toFileName) {
        if (isEmptyOrBlank(fromFilePath) || isEmptyOrBlank(toFileDir)) {
            return null;
        }
        String toFilePath = copyFile(fromFilePath, toFileDir, toFileName);
        if (!isEmptyOrBlank(toFilePath)) {
            removeFile(fromFilePath);
            return toFilePath;
        }
        return null;
    }

    /**
     * 剪切文件
     */
    public static void moveDir(String fromDirPath, String toDirPath) {
        if (isEmptyOrBlank(fromDirPath) || isEmptyOrBlank(toDirPath)) {
            return;
        }
        copyFile(fromDirPath, toDirPath);
        removeFile(fromDirPath);
    }

    /**
     * 把Uri文件保存到指定地址中
     */
    public static boolean saveFileWithUri(Context context, Uri fromUri, String toFilePath) {
        return saveFileWithUri(context, fromUri, getFileDirectory(toFilePath), getFileName(toFilePath));
    }

    /**
     * 返回Uri的对应文件信息（名称、类型，文件大小）
     */
    public static String[] getFileInfoWithUri(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.SIZE}, null, null, null);
        if (cursor == null) {
            return null;
        }
        if (cursor.moveToFirst()) {
            String fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME));
            String mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
            String fileSize = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE));
            if (!cursor.isClosed()) {
                cursor.close();
            }
            String[] info = {fileName, mimeType, fileSize};
            return info;
        }
        return null;
    }

    /**
     * 返回Uri的对应后缀名
     */
    public static String getFileSuffixWithUri(Context context, Uri uri) {
        String[] info = getFileInfoWithUri(context, uri);
        if (info == null || info.length < 1) {
            return null;
        }
        return getFileSuffix(info[0]);
    }

    /**
     * 返回Uri的对应文件大小
     */
    public static Long getFileSizeWithUri(Context context, Uri uri) {
        String[] info = getFileInfoWithUri(context, uri);
        if (info == null || info.length < 3) {
            return null;
        }
        try {
            return Long.parseLong(info[2]);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 把Uri文件保存到指定地址中
     */
    public static boolean saveFileWithUri(Context context, Uri fromUri, String toFileDir, String toFileName) {
        if (context == null || fromUri == null || isEmptyOrBlank(toFileDir) || isEmptyOrBlank(toFileName)) {
            return false;
        }
        mkdirIfNotExists(toFileDir);
        toFileDir = getSupplementaryDirPath(toFileDir);
        File autoSaveImage = new File(toFileDir + toFileName);
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(fromUri);
            outputStream = new FileOutputStream(autoSaveImage);
            int byteCount = 0;
            byte[] bytes = new byte[1024];
            while ((byteCount = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, byteCount);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 判断文件类型（通过魔数）
     */
    public static FileType getType(String filePath) throws IOException {
        // 获取文件头
        String fileHead = getFileHeader(filePath);

        if (fileHead != null && fileHead.length() > 0) {
            fileHead = fileHead.toUpperCase();
            FileType[] fileTypes = FileType.values();

            for (FileType type : fileTypes) {
                if (fileHead.startsWith(type.getValue())) {
                    return type;
                }
            }
        }

        return null;
    }

    /**
     * 读取文件头
     */
    private static String getFileHeader(String filePath) throws IOException {
        byte[] b = new byte[28];
        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(filePath);
            inputStream.read(b, 0, 28);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return bytesToHex(b);
    }

    /**
     * 将字节数组转换成16进制字符串
     */
    public static String bytesToHex(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public interface ProgressListener {
        void update(int progress);
    }

    /**
     * 写入处理接口
     * 请在onWriterProcess中写入
     */
    public interface WriterProcess<T> {
        public void onWriter(T writer) throws IOException;
    }
}
