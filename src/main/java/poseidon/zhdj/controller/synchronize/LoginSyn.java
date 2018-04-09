package poseidon.zhdj.controller.synchronize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import poseidon.zhdj.controller.BaseController;
import poseidon.zhdj.db.global.GlobalConstants;
import poseidon.zhdj.db.video.model.UserModel;
import poseidon.zhdj.db.video.service.UserService;
import poseidon.lib.core.tool.cookie.CookieTool;
import poseidon.lib.core.tool.date.DateTool;
import poseidon.lib.core.tool.string.StringTool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

@Controller
public class LoginSyn extends BaseController {
    private final static Logger log = LoggerFactory.getLogger(LoginSyn.class);

    String userInfoInterface;

    @Value("#{config['userInfoInterface']}")
    public void setLoginInterface(String val) {
        this.userInfoInterface = val;
    }

    @Autowired
    UserService userService;

    @RequestMapping(value = {"/login"})
    public String login(HttpServletRequest request, HttpServletResponse response, String acttype, String iv_user){
        log.debug("----------------------Enter login acttype = " + acttype + " & iv_user = " + iv_user);
        if(StringTool.isEmpty(acttype) || StringTool.isEmpty(iv_user)){
            return null;
        }
        //获取登陆者的信息并保存
        try {
            if(getUserInfo(request,response,iv_user)){
                if(GlobalConstants.ACT_QCJ == Integer.valueOf(acttype).intValue()){
                    return "forward:/zhdj";
                } else if(GlobalConstants.ACT_HZB == Integer.valueOf(acttype).intValue()){
                    return "forward:/zhdj";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    //             code”: 1,            code 1 调用成功 0调用失败 （两种情况：1 IP不在有效范围 2无此帐号）
    //            “userId”: “”,			//用户帐号
    //            “userName”:””,		//用户名称
    //            “avatar”:””,			//用户头像
    //            “deptName”:””,        //部门
    private boolean getUserInfo(HttpServletRequest request,HttpServletResponse response, String iv_user) throws IOException{
        //获取我们系统当前用户
        String qcj_uid = CookieTool.getCookieByName(request,"qcj_uid");
        if(StringTool.isNotEmpty(iv_user) && StringTool.isNotEmpty(qcj_uid)){
            if(!iv_user.equalsIgnoreCase(qcj_uid)){//只有当用户变了（不等于）的时候删掉原来的
                CookieTool.deleteCookie(response,request);
                request.setAttribute("qcj_uid",iv_user);
            }
        }
        String url = userInfoInterface + iv_user;
        StringBuilder sb = new StringBuilder();
        URL urlObject = new URL(url);
        URLConnection uc = urlObject.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String str = null;
        while ((str = in.readLine()) != null) {
            sb.append(str);
        }
        in.close();
        net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(sb.toString());
        log.debug("----------------------Get User Json Info =" + sb.toString());
        if(jsonObject.getInt("code") == 1){
            String userName = jsonObject.getString("userName");
            String userId = jsonObject.getString("userId");
            String avatar = jsonObject.getString("avatar");
            String deptName = jsonObject.getString("deptName");
            String unitName = jsonObject.getString("unitName");

            CookieTool.addCookie(response, "qcj_uid", userId);
            CookieTool.addCookie(response, "qcj_userName", userName);
            request.setAttribute("qcj_userName",userName);
            UserModel userModel = new UserModel();
            userModel.setUid(userId);
            userModel.setLoginTime(DateTool.now());
            userModel.setUserName(userName);
            userModel.setHeadImg(avatar);
            userModel.setDepartment(deptName);
            userModel.setUnitName(unitName);
            userService.saveUser(userModel);
            log.debug("----------------------Save User Info Successfully-------------------");
            return true;
        }
        return false;

    }




}
