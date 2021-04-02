package com.zyxx.email.common.bean;

import java.time.ZonedDateTime;

public class FileInfo {
    //文件所在bucket
    private String BucketName;
    //文件名
    private String FileName;
    //文件路径
    private String FilePath;
    //文件大小
    private String FileSize;
    //文件创建时间
    private ZonedDateTime CreateTime;
    //文件url
    private String FileUrl;
    //文件格式否支持预览
    private String FileType;
    //文件夹名
    private String FileFolder;
    public String getBucketName() {
        return BucketName;
    }
    public void setBucketName(String BucketName) {
        this.BucketName = BucketName;
    }
    public String getFileName() {
        return FileName;
    }
    public void setFileName(String FileName) {
        this.FileName = FileName;
    }
    public String getFilePath() {
        return FilePath;
    }
    public void setFilePath(String FilePath) {
        this.FilePath = FilePath;
    }
    public String getFileSize() {
        return FileSize;
    }
    public void setFileSize(String FileSize) {
        this.FileSize = FileSize;
    }
    public ZonedDateTime getCreateTime() {
        return CreateTime;
    }
    public void setCreateTime(ZonedDateTime CreateTime) {
        this.CreateTime = CreateTime;
    }
    public String getFileUrl() {
        return FileUrl;
    }
    public void setFileUrl(String FileUrl) {
        this.FileUrl = FileUrl;
    }
    public String getFileType() {
        return FileType;
    }
    public void setFileType(String FileType) {
        this.FileType = FileType;
    }
    public String getFileFolder() {
        return FileFolder;
    }
    public void setFileFolder(String FileFolder) {
        this.FileFolder = FileFolder;
    }
}
