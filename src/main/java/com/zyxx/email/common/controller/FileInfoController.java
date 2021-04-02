package com.zyxx.email.common.controller;

import com.zyxx.email.common.bean.FileInfo;
import com.zyxx.email.common.minio.MinioUtils;
import io.minio.messages.Bucket;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.List;

@Controller
@Api(value = "文件处理接口", tags = "FileInfoController", description = "文件处理相关")
public class FileInfoController {

    @Autowired
    private MinioUtils minioUtils;

    /**
     * 获取所有文件信息
     *
     * @return
     */
    @SneakyThrows
    @PostMapping("getallFileInfo")
    @ResponseBody
    @ApiOperation(value = "获取所有文件信息", notes = "获取所有bucket下的文件信息")
    public String getallFileInfo() throws IOException{
        List<Bucket> lb=minioUtils.getAllBuckets();
        JSONArray jsonArray=new JSONArray();//创建JSONArray对象
        for(int i=0;i<lb.size();i++){
            List<FileInfo> fileInfos=minioUtils.getAllFiles(lb.get(i).name());
            WriteJson(jsonArray, fileInfos);
        }
        return "{\"code\":0,\"msg\":\"\",\"count\":"+jsonArray.size()+",\"data\":"+jsonArray.toString()+"}";
    }

    private void WriteJson(JSONArray jsonArray, List<FileInfo> fileInfos) {
        for(int j=0;j<fileInfos.size();j++) {
            JSONObject jsonObject=new JSONObject();//创建JSONObject对象
            jsonObject.put("bucketname",fileInfos.get(j).getBucketName());
            jsonObject.put("filename",fileInfos.get(j).getFileName());
            jsonObject.put("filepath",fileInfos.get(j).getFilePath());
            jsonObject.put("size",fileInfos.get(j).getFileSize());
            jsonObject.put("createtime",fileInfos.get(j).getCreateTime());
            jsonObject.put("fileurl",fileInfos.get(j).getFileUrl());
            jsonObject.put("filetype",fileInfos.get(j).getFileType());
            jsonObject.put("filefolder",fileInfos.get(j).getFileFolder());
            jsonArray.add(jsonObject);//将jsonObject对象旁如jsonarray数组中
        }
    }

    /**
     * 获取空表格
     *
     * @return
     */
    @PostMapping("getNull")
    @ResponseBody
    @ApiOperation(value = "获取空表格", notes = "将json数据设置为空")
    public String getNull() throws IOException {
        String json = "{\"code\":0,\"msg\":\"\",\"count\":0,\"data\":[]}";
        return json;
    }

    /**
     * 获取所有bucketname
     *
     * @return
     */
    @SneakyThrows
    @PostMapping("getallBucketName")
    @ResponseBody
    @ApiOperation(value = "获取所有bucketname", notes = "获取所有bucketname信息")
    public String getallBucketName() throws IOException {
        List<Bucket> lb=minioUtils.getAllBuckets();
        JSONArray jsonArray=new JSONArray();//创建JSONArray对象
        for(int i=0;i<lb.size();i++){
            JSONObject jsonObject=minioUtils.addChildren(lb.get(i).name(),lb.get(i).name(),lb.get(i).name());
            jsonArray.add(jsonObject);
        }
        return jsonArray.toString();
    }


    /**
     * 根据用户点击菜单项获取bucketname
     *
     * @param bucketname
     * @return
     */
    @PostMapping("getBucketName")
    @ResponseBody
    @ApiOperation(value = "获取bucketname", notes = "根据用户点击菜单项获取bucketname")
    public String getBucketName(@RequestParam("value") String bucketname) throws IOException {
        return bucketname;
    }

    /**
     * 返回某一文件夹下所有文件
     *
     * @param bucketname
     * @param filefolder
     * @return
     */
    @SneakyThrows
    @PostMapping("getFileFolder")
    @ResponseBody
    @ApiOperation(value = "获取文件夹下的文件信息", notes = "获取某一文件夹下的文件信息")
    public String getFileFolder(@RequestParam("value1") String bucketname,@RequestParam("value2") String filefolder) throws IOException{
        JSONArray jsonArray = new JSONArray();//创建JSONArray对象
        List<FileInfo> fileInfos = minioUtils.getAllObjectsByPrefix(bucketname, filefolder, true);
        return getString(jsonArray, fileInfos);
    }

    /**
     * 获取某一bucket下的文件信息
     *
     * @param bucketname
     * @return
     */
    @SneakyThrows
    @PostMapping("getFileInfo")
    @ResponseBody
    @ApiOperation(value = "获取某一bucket下的文件信息", notes = "根据得到的bucketname获取该bucket下的文件信息")
    public String getFileInfo(@RequestParam("value1") String bucketname,@RequestParam("value2") String filefolder) throws IOException {
        JSONArray jsonArray=new JSONArray();//创建JSONArray对象
        List<FileInfo> fileInfos;
        if(bucketname.equals(filefolder)) {
            fileInfos = minioUtils.getAllFiles(bucketname);
        }
        else{
            fileInfos = minioUtils.getAllObjectsByPrefix(bucketname, filefolder, true);
        }
        return getString(jsonArray, fileInfos);
    }

    @NotNull
    private String getString(JSONArray jsonArray, List<FileInfo> fileInfos) throws JSONException {
        WriteJson(jsonArray, fileInfos);
        return "{\"code\":0,\"msg\":\"\",\"count\":"+jsonArray.size()+",\"data\":"+jsonArray.toString()+"}";
    }
}
