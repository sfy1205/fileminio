package com.zyxx.email.common.controller;

import com.alibaba.fastjson.JSONObject;
import com.zyxx.email.common.bean.FileInfo;
import com.zyxx.email.common.minio.MinioUtils;
import io.micrometer.core.instrument.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

@Controller
@Api(value = "文件上传下载接口", tags = "MinioController", description = "文件上传下载相关")
public class MinioController {

    @Autowired
    private MinioUtils minioUtils;

    @GetMapping("init")
    @ApiOperation(value = "文件管理平台首页", notes = "文件上传预览及下载页")
    public String init() throws IOException {
        return "layui/file";
    }

    @GetMapping("init1")
    @ApiOperation(value = "文件管理平台", notes = "文件上传预览及下载")
    public String init1() throws IOException {
        return "layui/file1";
    }

    @GetMapping("init3")
    @ApiOperation(value = "文件管理平台", notes = "文件上传预览及下载")
    public String init3() throws IOException {
        return "layui/file3";
    }

    /**
     * 输入bucketname
     *
     * @param bucketname
     * @return
     */
    @PostMapping("getInputBucketName")
    @ResponseBody
    @ApiOperation(value = "获取要上传的bucketname", notes = "根据用户输入值获取要上传的bucketname")
    public String getInputBucketName(@RequestParam("value") String bucketname) throws IOException {
        return bucketname;
    }

    /**
     * 输入文件夹名
     *
     * @param filefolder
     * @return
     */
    @PostMapping("getInputPid")
    @ResponseBody
    @ApiOperation(value = "获取要上传的文件夹", notes = "根据用户输入值获取要上传的文件夹")
    public String getInputPid(@RequestParam("value") String filefolder) throws IOException {
        return filefolder;
    }

    /**
     * 上传文件
     *
     * @param file
     * @param bucketname
     * @return
     */
    @PostMapping(value = "/upload")
    @ResponseBody
    @ApiOperation(value = "上传文件", notes = "根据用户输入的bucketname上传文件")
    public String upload(@RequestParam(name = "file", required = false) MultipartFile file, String bucketname,String folder) throws IOException {
        bucketname= URLDecoder.decode(bucketname, "utf-8");
        folder= URLDecoder.decode(folder, "utf-8");
        JSONObject res=new JSONObject();
        try {
            String originalFilename = file.getOriginalFilename();
            String fileName;
            if(bucketname.equals(folder)){
                fileName = originalFilename;
            }
            else {
                fileName = folder.replace(bucketname,"") + "/" + originalFilename;
            }
            if(minioUtils.doesObjectExist(bucketname,fileName)){
                res.put("code", 0);
                res.put("msg", "文件已存在");
            }
            else {
                minioUtils.putObject(bucketname, file, fileName, file.getContentType());
                res.put("code", 1);
                res.put("msg", "上传成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.put("code", 0);
            res.put("msg", "上传失败");
        }
        return res.toJSONString();
    }

    /**
     * 删除文件
     *
     * @param bucketname
     * @param objectname
     * @return
     */
    @PostMapping(value = "/delete")
    @ResponseBody
    @ApiOperation(value = "删除文件", notes = "根据bucketname及文件名从minIO服务器中删除文件")
    public String delete(@RequestParam("bucketname") String bucketname, @RequestParam("objectname") String objectname) {
        try {
            minioUtils.removeObject(bucketname, objectname);
        } catch (Exception e) {
            return "删除失败" + e.getMessage();
        }
        return "删除成功";
    }

    /**
     * 下载文件
     *
     * @param fileUrl  文件绝对路径
     * @param response
     * @throws IOException
     */
    @GetMapping("downloadFile")
    @ApiOperation(value = "下载文件", notes = "根据文件url下载文件")
    public void downloadFile(String fileUrl, HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(fileUrl)) {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            String data = "文件下载失败";
            OutputStream ps = response.getOutputStream();
            ps.write(data.getBytes("UTF-8"));
            return ;
        }
        try {
            // 拿到文件路径
            String url = fileUrl.split("9000/")[1];
            // 获取文件对象
            String bucketname = url.split("/")[0];
            InputStream object = minioUtils.getObject(bucketname, url.substring(url.indexOf("/") + 1));
            byte buf[] = new byte[1024];
            int length = 0;
            response.reset();
            response.setHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(url.substring(url.lastIndexOf("/") + 1), "UTF-8"));
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("UTF-8");
            OutputStream outputStream = response.getOutputStream();
            // 输出文件
            while ((length = object.read(buf)) > 0) {
                outputStream.write(buf, 0, length);
            }
            // 关闭输出流
            outputStream.close();
        } catch (Exception ex) {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            String data = "文件下载失败";
            OutputStream ps = response.getOutputStream();
            ps.write(data.getBytes("UTF-8"));
        }
    }

    /**
     * 预览文件
     *
     * @param fileUrl  文件绝对路径
     * @param response
     * @throws IOException
     */
    @GetMapping("PreviewFile")
    @ApiOperation(value = "预览文件", notes = "根据文件url预览文件")
    public void PreviewFile(String fileUrl, HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(fileUrl)) {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            String data = "文件预览失败";
            OutputStream ps = response.getOutputStream();
            ps.write(data.getBytes("UTF-8"));
            return ;
        }
        try {
            // 拿到文件路径
            String url = fileUrl.split("9000/")[1];
            // 获取文件对象
            String bucketname = url.split("/")[0];
            InputStream object = minioUtils.getObject(bucketname, url.substring(url.indexOf("/") + 1));
            byte buf[] = new byte[1024];
            int length = 0;
            response.reset();
            response.setHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(url.substring(url.lastIndexOf("/") + 1), "UTF-8"));
            OutputStream outputStream = response.getOutputStream();
            // 输出文件
            while ((length = object.read(buf)) > 0) {
                outputStream.write(buf, 0, length);
            }
            // 关闭输出流
            outputStream.close();
        } catch (Exception ex) {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            String data = "文件预览失败";
            OutputStream ps = response.getOutputStream();
            ps.write(data.getBytes("UTF-8"));
        }
    }

    /**
     * 创建bucket
     *
     * @param bucketname
     * @return
     */
    @SneakyThrows
    @PostMapping(value = "/CreateBucket")
    @ResponseBody
    @ApiOperation(value = "创建bucket", notes = "创建bucket")
    public String CreateBucket(@RequestParam("bucketname") String bucketname) {
        JSONObject res=new JSONObject();
        try {
            if(!minioUtils.bucketExists(bucketname)){
                minioUtils.createBucket(bucketname);
                res.put("code", 1);
                res.put("msg", "创建成功");
            }
            else{
                res.put("code", 0);
                res.put("msg", "bucket已存在");
            }
        } catch (Exception e) {
            res.put("code", 0);
            res.put("msg", "创建失败,bucket名非法");
        }
        return res.toJSONString();
    }

    /**
     * 创建文件夹
     *
     * @param bucketname
     * @param filefolder
     * @return
     */
    @PostMapping(value = "/createfolder")
    @ResponseBody
    @ApiOperation(value = "创建文件夹", notes = "在bucket下创建文件夹")
    public String createfolder(@RequestParam("bucketname") String bucketname, @RequestParam("filefolder") String filefolder) {
        try {
            minioUtils.putDirObject(bucketname, filefolder);
        } catch (Exception e) {
            return "创建失败" + e.getMessage();
        }
        return "创建成功";
    }

    /**
     * 删除文件夹
     *
     * @param bucketname
     * @param filefolder
     * @return
     */
    @SneakyThrows
    @PostMapping(value = "/DeleteFilefolder")
    @ResponseBody
    @ApiOperation(value = "删除文件夹", notes = "根据bucketname及文件夹名删除文件夹")
    public String DeleteFilefolder(@RequestParam("bucketname") String bucketname,@RequestParam("filefolder") String filefolder) {
        JSONObject res = new JSONObject();
        try {
            List<FileInfo> fileInfos = minioUtils.getAllObjectsByPrefix(bucketname, filefolder, true);
            for (int i = 0; i < fileInfos.size(); i++) {
                minioUtils.removeObject(bucketname, fileInfos.get(i).getFilePath());
            }
            res.put("code", 1);
            res.put("msg", "删除成功");
        } catch (Exception e) {
            res.put("code", 0);
            res.put("msg", "删除失败");
        }
        return res.toJSONString();
    }

    /**
     * 删除bucket
     *
     * @param bucketname
     * @return
     */
    @SneakyThrows
    @PostMapping(value = "/DeleteBucket")
    @ResponseBody
    @ApiOperation(value = "删除bucket", notes = "根据bucketname删除bucket")
    public String DeleteBucket(@RequestParam("bucketname") String bucketname) {
        JSONObject res=new JSONObject();
        try {
            if(minioUtils.bucketExists(bucketname)){
                List<FileInfo> fileInfos=minioUtils.getAllFiles(bucketname);
                for(int i=0;i<fileInfos.size();i++) {
                    minioUtils.removeObject(bucketname, fileInfos.get(i).getFilePath());
                }
                minioUtils.removeBucket(bucketname);
                res.put("code", 1);
                res.put("msg", "删除成功");
            }
            else{
                res.put("code", 0);
                res.put("msg", "bucket不存在");
            }
        } catch (Exception e) {
            res.put("code", 0);
            res.put("msg", "删除失败");
        }
        return res.toJSONString();
    }

    /**
     * 上传文件夹
     *
     * @param bucketname
     * @return
     */
    @PostMapping(value = "/uploadFolder")
    @ResponseBody
    @ApiOperation(value = "上传文件", notes = "根据用户输入的bucketname上传文件")
    public String uploadFolder(String bucketname,String foldername,MultipartFile[] folder) {
        MinioUtils.uploadFolder("bbb","ccc", folder);
        return "ok";
    }
}