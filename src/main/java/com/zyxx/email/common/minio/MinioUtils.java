package com.zyxx.email.common.minio;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zyxx.email.common.bean.FileInfo;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * @Author: zrs
 * @Date: 2020/12/01/10:02
 * @Description: Minio工具类
 */
@Slf4j
public class MinioUtils {

    private static MinioClient minioClient;

    private static String endpoint;
    private static String accessKey;
    private static String secretKey;

    private MinioUtils() {
    }

    public MinioUtils(String endpoint, String accessKey, String secretKey) {
        MinioUtils.endpoint = endpoint;
        MinioUtils.accessKey = accessKey;
        MinioUtils.secretKey = secretKey;
        createMinioClient();
    }

    /**
     * 创建minioClient
     */
    public void createMinioClient() {
        try {
            if (null == minioClient) {
                log.info("minioClient create start");
                minioClient = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey)
                        .build();
                log.info("minioClient create end");
            }
        } catch (Exception e) {
            log.error("连接MinIO服务器异常：{}", e);
        }
    }

    /**
     * 验证bucketName是否存在
     *
     * @return boolean true:存在
     */
    public static boolean bucketExists(String bucketName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    /**
     * 创建bucket
     *
     * @param bucketName bucket名称
     */
    public static void createBucket(String bucketName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException, RegionConflictException {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(bucketjson(bucketName)).build());
        }
    }

    public static String bucketjson(String bucketname){
        String string="{\n" +
                "\t\"Version\": \"2012-10-17\",\n" +
                "\t\"Statement\": [{\n" +
                "\t\t\"Effect\": \"Allow\",\n" +
                "\t\t\"Principal\": {\n" +
                "\t\t\t\"AWS\": [\"*\"]\n" +
                "\t\t},\n" +
                "\t\t\"Action\": [\"s3:GetBucketLocation\", \"s3:ListBucket\", \"s3:ListBucketMultipartUploads\"],\n" +
                "\t\t\"Resource\": [\"arn:aws:s3:::"+bucketname+"\"]\n" +
                "\t}, {\n" +
                "\t\t\"Effect\": \"Allow\",\n" +
                "\t\t\"Principal\": {\n" +
                "\t\t\t\"AWS\": [\"*\"]\n" +
                "\t\t},\n" +
                "\t\t\"Action\": [\"s3:AbortMultipartUpload\", \"s3:DeleteObject\", \"s3:GetObject\", \"s3:ListMultipartUploadParts\", \"s3:PutObject\"],\n" +
                "\t\t\"Resource\": [\"arn:aws:s3:::"+bucketname+"/*\"]\n" +
                "\t}]\n" +
                "}\n" +
                "————————————————\n";
        return string;
    }

    /**
     * 获取存储桶策略
     *
     * @param bucketName 存储桶名称
     * @return json
     */
    private JSONObject getBucketPolicy(String bucketName)
            throws IOException, InvalidKeyException, InvalidResponseException, BucketPolicyTooLargeException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, InsufficientDataException, ErrorResponseException {
        String bucketPolicy = minioClient
                .getBucketPolicy(GetBucketPolicyArgs.builder().bucket(bucketName).build());
        return JSONObject.parseObject(bucketPolicy);
    }

    /**
     * 获取全部bucket
     * <p>
     * https://docs.minio.io/cn/java-client-api-reference.html#listBuckets
     */
    public static List<Bucket> getAllBuckets()
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.listBuckets();
    }

    /**
     * 根据bucketName获取信息
     *
     * @param bucketName bucket名称
     */
    public static Optional<Bucket> getBucket(String bucketName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.listBuckets().stream().filter(b -> b.name().equals(bucketName)).findFirst();
    }

    /**
     * 根据bucketName删除信息
     *
     * @param bucketName bucket名称
     */
    public static void removeBucket(String bucketName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    }


    /**
     * 判断文件是否存在
     *
     * @param bucketName 存储桶
     * @param objectName 对象
     * @return true：存在
     */
    public static boolean doesObjectExist(String bucketName, String objectName) {
        boolean exist = true;
        try {
            minioClient
                    .statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            exist = false;
        }
        return exist;
    }

    /**
     * 判断文件夹是否存在
     *
     * @param bucketName 存储桶
     * @param objectName 文件夹名称（去掉/）
     * @return true：存在
     */
    public static boolean doesFolderExist(String bucketName, String objectName) {
        boolean exist = false;
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(objectName).recursive(false).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir() && objectName.equals(item.objectName())) {
                    exist = true;
                }
            }
        } catch (Exception e) {
            exist = false;
        }
        return exist;
    }

    /**
     * 根据文件夹名获取文件信息
     *
     * @param bucketName bucket名称
     * @param prefix 前缀
     * @param recursive 是否递归查询
     * @return MinioItem 列表
     */
    @SneakyThrows
    public static List<FileInfo> getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) {
        List<FileInfo> fileInfos=new ArrayList<>();
        DecimalFormat df=new DecimalFormat("######0.00");
        Iterable<Result<Item>> objectsIterator = minioClient.listObjects(bucketName,prefix,recursive);
        if (objectsIterator != null) {
            WriteFileInfo(bucketName, df, fileInfos, objectsIterator);
        }
        return fileInfos;
    }

    /**
     * 判断该级文件夹下是否有其他文件夹存在
     *
     * @param filefolder 文件夹名
     * @param fileInfos 文件夹内的文件列表
     * @return
     */
    public static boolean hasChildren(String filefolder,List<FileInfo> fileInfos) {
        for(int i=0;i<fileInfos.size();i++){
            if(fileInfos.get(i).getFileFolder().length()>filefolder.length()+1){
                return true;
            }
        }
        return false;
    }

    /**
     * 递归生成json数据
     *
     * @param filefolder 文件夹名
     * @param pfilefolder 父文件夹名
     * @param bucketname bucket名
     * @return
     */
    public static JSONObject addChildren(String bucketname,String pfilefolder,String filefolder) {
        JSONObject jsonObject = new JSONObject();
        int num = filefolder.split("/").length - pfilefolder.split("/").length;
        for (int i = 0; i < num - 1; i++) {
            filefolder=filefolder.replaceAll(filefolder.split("/")[filefolder.split("/").length - 1]+"/", "");
        }
        jsonObject.put("path",filefolder);
        jsonObject.put("filefolder",filefolder.split("/")[filefolder.split("/").length-1]);
        jsonObject.put("folder",filefolder.split("/")[filefolder.split("/").length-1]);
        List<FileInfo> fileInfos;
        if(filefolder.split("/").length==1){
            fileInfos = getAllFiles(bucketname);
        }
        else {
            String newfilefolder=filefolder.replaceAll(filefolder.split("/")[0]+"/","");
            fileInfos = getAllObjectsByPrefix(bucketname, newfilefolder, true);
        }
        if(hasChildren(filefolder,fileInfos)){
            JSONArray jsonArray=new JSONArray();
            for(int i=0;i<fileInfos.size();i++){
                if(fileInfos.get(i).getFileFolder().split("/").length>filefolder.split("/").length) {
                    JSONObject jsonObject1 = addChildren(bucketname,filefolder,fileInfos.get(i).getFileFolder());
                    jsonArray.add(jsonObject1);
                }
            }
            jsonArray=DuplicateRemoval(jsonArray);
            jsonObject.put("children",jsonArray);
        }
        return jsonObject;
    }

    public static JSONArray DuplicateRemoval(JSONArray jsonArray) {
        for(int i=0;i<jsonArray.size();i++){
            for(int j=jsonArray.size()-1;j>i;j--){
                if(jsonArray.get(j).equals(jsonArray.get(i))){
                    jsonArray.remove(j);
                }
            }
        }
        return jsonArray;
    }

    /**
     * 根据文件夹名获取该目录下的所有文件信息
     *
     * @param fileFolder 文件夹名称
     * @return MinioItem 列表
     */
    @SneakyThrows
    public static List<FileInfo> getAllObjectsByfileName(String fileFolder) {
        String bucketName=fileFolder.split("/")[0];
        String prefix=fileFolder.replaceAll(bucketName,"");
        List<FileInfo> fileInfos = getAllObjectsByPrefix(bucketName,prefix,true);
        return fileInfos;
    }

    /**
     * 获取bucket中全部文件信息
     *
     * @param bucketName 存储桶
     * @return
     */
    @SneakyThrows
    public static List<FileInfo> getAllFiles(String bucketName) {
        DecimalFormat df=new DecimalFormat("######0.00");
        List<FileInfo> fileInfos=new ArrayList<>();
        Iterable<Result<Item>> results=minioClient.listObjects(bucketName);
        WriteFileInfo(bucketName, df, fileInfos, results);
        return fileInfos;
    }

    private static void WriteFileInfo(String bucketName, DecimalFormat df, List<FileInfo> fileInfos, Iterable<Result<Item>> results) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidBucketNameException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        for(Result<Item> result :results) {
            Item item= result.get();
            FileInfo fileInfo=new FileInfo();
            fileInfo.setBucketName(bucketName);
            String filename=URLDecoder.decode(item.objectName(),"utf-8");
            fileInfo.setFileName(filename.split("/")[(filename.split("/")).length-1]);
            fileInfo.setFilePath(filename);
            if(item.size()<1024) {
                fileInfo.setFileSize(item.size() + "bytes");
            }
            else if(item.size()>=1048576) {
                fileInfo.setFileSize(df.format((item.size() / 1048576.0)) + "MB");
            }
            else{
                fileInfo.setFileSize(df.format((item.size() / 1024.0)) + "KB");
            }
            fileInfo.setCreateTime(item.lastModified().plusHours(8));
            fileInfo.setFileUrl("\\"+bucketName +"\\"+filename);
            fileInfo.setFileType(SupportPreview(item.objectName()));
            String objectname=URLDecoder.decode(item.objectName());
            fileInfo.setFileFolder(bucketName+"/"+objectname.replace(objectname.split("/")[(objectname.split("/")).length-1],""));
            fileInfos.add(fileInfo);
        }
    }

    /**
     * 获取文件流
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 二进制流
     */
    public static InputStream getObject(String bucketName, String objectName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient
                .getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    /**
     * 断点下载
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param offset 起始字节的位置
     * @param length 要读取的长度
     * @return 流
     */
    public InputStream getObject(String bucketName, String objectName, long offset, long length)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectName).offset(offset).length(length)
                        .build());
    }

    /**
     * 获取路径下文件列表
     *
     * @param bucketName bucket名称
     * @param prefix 文件名称
     * @param recursive 是否递归查找，如果是false,就模拟文件夹结构查找
     * @return 二进制流
     */
    public static Iterable<Result<Item>> listObjects(String bucketName, String prefix,
                                                     boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(recursive).build());
    }

    /**
     * 通过MultipartFile，上传文件
     *
     * @param bucketName 存储桶
     * @param file 文件
     * @param objectName 对象名
     */
    public static ObjectWriteResponse putObject(String bucketName, MultipartFile file,
                                                String objectName, String contentType)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        InputStream inputStream = file.getInputStream();
        return minioClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(objectName).contentType(contentType)
                        .stream(
                                inputStream, inputStream.available(), -1)
                        .build());
    }

    /**
     * 上传文件夹
     *
     * @param bucketName bucket名
     * @param filefolder 上传的文件夹名
     * @param files 文件夹中的文件
     */
    @SneakyThrows
    public static void uploadFolder(String bucketName, String filefolder, MultipartFile[] files) {
        File file=multipartFileToFile(files[0]);
        String filepath=file.getAbsolutePath();//获取文件路径
        String foldername=filepath.split("/")[filepath.split("/").length-1];//截取文件夹名(最后一个/后的字符串)
        for(int i=0;i<files.length;i++){
         putObject(bucketName,files[i],files[i].getName()+i,files[i].getContentType());
         }
    }

    @SneakyThrows
    public static File multipartFileToFile(MultipartFile file){
        File toFile=null;
        if(file.equals("")||(file.getSize()<=0)){
            file=null;
        }
        else{
            InputStream ins=null;
            ins=file.getInputStream();
            toFile=new File(file.getOriginalFilename());
            inputStreamToFile(ins,toFile);
            ins.close();
        }
        return toFile;
    }

    private static void inputStreamToFile(InputStream ins, File file){
        try{
            OutputStream os=new FileOutputStream(file);
            int bytesRead=0;
            byte[] buffer=new byte[8192];
            while((bytesRead=ins.read(buffer,0,8192))!=-1){
                os.write(buffer,0,bytesRead);
            }
            os.close();
            ins.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 上传本地文件
     *
     * @param bucketName 存储桶
     * @param objectName 对象名称
     * @param fileName 本地文件路径
     */
    public static ObjectWriteResponse putObject(String bucketName, String objectName,
                                                String fileName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucketName).object(objectName).filename(fileName).build());
    }

    /**
     * 通过流上传文件
     *
     * @param bucketName 存储桶
     * @param objectName 文件对象
     * @param inputStream 文件流
     */
    public static ObjectWriteResponse putObject(String bucketName, String objectName,
                                                InputStream inputStream)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                        inputStream, inputStream.available(), -1)
                        .build());
    }

    /**
     * 创建文件夹或目录
     *
     * @param bucketName 存储桶
     * @param objectName 目录路径
     */
    public static ObjectWriteResponse putDirObject(String bucketName, String objectName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                        new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build());
    }

    /**
     * 获取文件信息, 如果抛出异常则说明文件不存在
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     */
    public static ObjectStat statObject(String bucketName, String objectName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient
                .statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    /**
     * 拷贝文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param srcBucketName 目标bucket名称
     * @param srcObjectName 目标文件名称
     */
    public static ObjectWriteResponse copyObject(String bucketName, String objectName,
                                                 String srcBucketName, String srcObjectName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.copyObject(
                CopyObjectArgs.builder()
                        .source(CopySource.builder().bucket(bucketName).object(objectName).build())
                        .bucket(srcBucketName)
                        .object(srcObjectName)
                        .build());
    }

    /**
     * 删除文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     */
    public static void removeObject(String bucketName, String objectName)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        minioClient
                .removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    /**
     * 批量删除文件
     *
     * @param bucketName bucket
     * @param keys 需要删除的文件列表
     * @return
     */
    public static void removeObjects(String bucketName, List<String> keys) {
        List<DeleteObject> objects = new LinkedList<>();
        keys.forEach(s -> {
            objects.add(new DeleteObject(s));
            try {
                removeObject(bucketName, s);
            } catch (Exception e) {
                log.error("批量删除失败！error:{}",e);
            }
        });
    }


    /**
     * 获取文件外链
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param expires 过期时间 <=7 秒级
     * @return url
     */
    public static String getPresignedObjectUrl(String bucketName, String objectName,
                                               Integer expires)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, InvalidExpiresRangeException, ServerException, InternalException, NoSuchAlgorithmException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.presignedGetObject(bucketName, objectName, expires);
    }

    /**
     * 给presigned URL设置策略
     *
     * @param bucketName 存储桶
     * @param objectName 对象名
     * @param expires 过期策略
     * @return map
     */
    public static Map<String, String> presignedGetObject(String bucketName, String objectName,
                                                         Integer expires)
            throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, InvalidExpiresRangeException, ServerException, InternalException, NoSuchAlgorithmException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        PostPolicy policy = new PostPolicy(bucketName, objectName,
                ZonedDateTime.now().plusDays(7));
        policy.setContentType("image/png");
        return minioClient.presignedPostPolicy(policy);
    }


    /**
     * 将URLDecoder编码转成UTF8
     *
     * @param str
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String getUtf8ByURLDecoder(String str) throws UnsupportedEncodingException {
        String url = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        return URLDecoder.decode(url, "UTF-8");
    }

    /**
     * 判断文件是否支持预览
     *
     * @param objectName 文件名
     * @return
     */
    public static String SupportPreview(String objectName) {
        String fileSuffix=objectName.substring(objectName.lastIndexOf("."));
        if (StringUtils.isBlank(fileSuffix)) {
            return "false";
        }
        String[] arr1 = {".bmp", ".dib", ".gif", ".jfif", ".jpe", ".jpeg", ".jpg", ".png", ".tif", ".tiff", ".ico"};//图片
        //String[] arr2 = {".rmvb",".mp4",".3gp",".mpeg",".wmv",".avi",".mov",".mpv",".swf",".flv"};//视频
        String[] arr3 = {".pdf",".txt",".xml",".html",".css",".js",".php",".c",".cpp",".h",".hpp",".json"};//文本
        //String[] arr4 = {".mp3",".midi",".wma",".cda",".wav",".flac"};//音频
        for (String item : arr1) {
            if (item.equals(fileSuffix)) {
                return "true";
            }
        }
        /*for (String item : arr2) {
            if (item.equals(fileSuffix)) {
                return "true";
            }
        }*/
        for (String item : arr3) {
            if (item.equals(fileSuffix)) {
                return "true";
            }
        }
        /*for (String item : arr4) {
            if (item.equals(fileSuffix)) {
                return "true";
            }
        }*/
        return "false";
    }

    /**
     * 确保目录存在，不存在则创建
     * @param filePath
     */
    private static void makeDir(String filePath) {
        if (filePath.lastIndexOf('/') > 0) {
            String dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }
}