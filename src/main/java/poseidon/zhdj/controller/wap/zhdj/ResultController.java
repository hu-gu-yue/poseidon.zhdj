package poseidon.zhdj.controller.wap.zhdj;

import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import poseidon.lib.core.tool.cookie.CookieTool;
import poseidon.lib.core.tool.date.DateTool;
import poseidon.lib.core.tool.string.Base64;
import poseidon.lib.core.tool.string.StringTool;
import poseidon.zhdj.controller.BaseController;
import poseidon.zhdj.db.video.model.UserModel;
import poseidon.zhdj.db.video.service.UserService;
import weixin.WeixinOauth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@Controller
public class ResultController extends BaseController{
	private final static Logger log = LoggerFactory.getLogger(ResultController.class);
	@Autowired
	UserService userService;

	String weixinlogin;
	@Value("#{config['weixinlogin']}")
	public void setWeixinlogin(String val) {
		this.weixinlogin = val;
	}

	String webServer;
	@Value("#{config['webServer']}")
	public void setWebServer(String val) {
		this.webServer = val;
	}

	private final static String APPTOKEN = "jmbMth0LC9MoF1kbiiHLZ9uoTMZItU7M";
	/**
	 * 进入青创菁评选活动
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping(value = {"/qcj/m/result.html" })
	public String pingXuan(HttpServletRequest request,HttpServletResponse response,Model model, String YiCoc) {
		//如果没有登录，调中烟的登录接口
		log.debug("--------------POST信息----------------YiCoc = " + YiCoc);
		if(!hasCookie(request, "qcj_uid") && StringTool.isEmpty(YiCoc)){
			return "redirect:"+ weixinlogin + Base64.encode(webServer + "qcj/m/result.html") ;
		} else if(hasCookie(request, "qcj_uid")){
			String uid = CookieTool.getCookieByName(request, "qcj_uid");
			model.addAttribute("qcjUid",uid);

			return "wap/zhdj/result.html";
		} else if(!hasCookie(request, "qcj_uid") && StringTool.isNotEmpty(YiCoc)){
			try {
				String jsonstr = URLDecoder.decode(YiCoc, "UTF-8");
				log.debug("--------------jsonstr----------------" + jsonstr);
				JSONObject jsonObject = JSONObject.fromObject(jsonstr);
				String openid = jsonObject.getString("openid");
				String avatar = URLDecoder.decode(jsonObject.getString("avatar"),"UTF-8");
				String realname = jsonObject.getString("realname");
				String encoding = jsonObject.getString("encoding");
				String timestamp = jsonObject.getString("timestamp");
				String gid = jsonObject.getString("gid");
				String bm = jsonObject.getString("bm");

				WeixinOauth weiAuth = new WeixinOauth();
				String sign = weiAuth.sha1(openid + APPTOKEN + gid + timestamp);
				log.debug("--------------校验----------------sign = " + sign + " encoding = " + encoding);

				if(sign.equals(encoding)){
					if(StringTool.isNotEmpty(avatar)){
						avatar = URLDecoder.decode(avatar,"UTF-8");
					}
					if(StringTool.isNotEmpty(realname)){
						realname = URLDecoder.decode(realname,"UTF-8");
					}
					UserModel userModel = new UserModel();
					userModel.setUid(openid);
					userModel.setLoginTime(DateTool.now());
					userModel.setUserName(realname);
					userModel.setHeadImg(avatar);
					userModel.setDepartment(bm);
					userService.saveUser(userModel);

					CookieTool.addCookie(response, "qcj_uid", openid);
					CookieTool.addCookie(response, "qcj_userName", realname);

					model.addAttribute("userName",realname);
					model.addAttribute("qcjUid",openid);
					return "wap/zhdj/result.html";
				}

			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		return "wap/zhdj/result.html";
	}

}
