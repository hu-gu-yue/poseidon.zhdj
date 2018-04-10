package poseidon.zhdj.controller.zhdj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import poseidon.lib.core.tool.cookie.CookieTool;
import poseidon.lib.core.tool.string.StringTool;
import poseidon.zhdj.controller.BaseController;
import poseidon.zhdj.db.global.GlobalConstants;
import poseidon.zhdj.db.video.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ZhiboController extends BaseController{
	private final static Logger log = LoggerFactory.getLogger(ZhiboController.class);
	@Autowired
	UserService userService;

	String loginInterface;
	@Value("#{config['loginInterface']}")
	public void setLoginInterface(String val) {
		this.loginInterface = val;
	}

	//用于连接socket
	String wsServer;
	@Value("#{config['wsServer']}")
	public void setWebServer(String val) {
		this.wsServer = val;
	}


	//结果公示页面
	@RequestMapping(value = { "/hezhubo"})
	public String hezhuboResult(HttpServletRequest request, Model model, Integer inner, HttpServletResponse response,@RequestParam(defaultValue = "1")String page) {
		if(StringTool.isEmpty(CookieTool.getCookieByName(request,"qcj_uid"))){
			CookieTool.addCookie(response,"qcj_uid","hexiaosheng");
			CookieTool.addCookie(response,"qcj_userName","和小生");
		}
		//如果没有登录，调中烟的登录接口
		if(!hasCookie(request, "qcj_uid")){
			if(null == inner){
				return "redirect:"+loginInterface + "&acttype=" + GlobalConstants.ACT_HZB;
			} else{
				return "redirect:"+loginInterface + "&acttype=" + GlobalConstants.ACT_HZB + "&inner=" + inner.toString();
			}
		}
		String uid = String.valueOf(null == request.getAttribute("qcj_uid") ? "" : request.getAttribute("qcj_uid"));
		String userName = String.valueOf(null == request.getAttribute("qcj_userName") ? "" : request.getAttribute("qcj_userName"));

		if(StringTool.isEmpty(uid)){
			uid = CookieTool.getCookieByName(request, "qcj_uid");
			userName = CookieTool.getCookieByName(request, "qcj_userName");
		}
		model.addAttribute("userName",userName);
		model.addAttribute("inner",inner);

		//更新首页访问量
		//获取总作品量  总浏览量 每个厂的视频数 龙虎榜数据
		//首页访问量
		Map<String,Object> paramMap = new HashMap<String,Object>();
		paramMap.put("act",GlobalConstants.ACT_HZB);
		paramMap.put("uid","");

		return "zhdj/publicResult.html";

	}

	/**
	 * 直播间详情
	 * @param request
	 * @param model
	 * @param vid
     * @return
     */
	@RequestMapping("hezhubo/zhibojian.html")
	public String liveRoom(HttpServletRequest request, Model model, Long vid, Integer inner) {
		String uid = CookieTool.getCookieByName(request, "qcj_uid");
		//如果没有登录，调中烟的登录接口
		if(!hasCookie(request, "qcj_uid")){
			if(null == inner){
				return "redirect:"+loginInterface + "&acttype=" + GlobalConstants.ACT_HZB;
			} else{
				return "redirect:"+loginInterface + "&acttype=" + GlobalConstants.ACT_HZB + "&inner=" + inner.toString();
			}
		}

		model.addAttribute("wsServer",wsServer);
		model.addAttribute("inner",inner);

        String userName = CookieTool.getCookieByName(request, "qcj_userName");
        model.addAttribute("userName",userName);

		return "zhdj/zhibojian.html";
	}

	/**
	 *  删除cookie
	 * @param response
	 * @param request
	 * @return
	 */
	@RequestMapping(value = {"/hezhubo/deleteCookie"})
	public Object deleteCookie(HttpServletResponse response ,HttpServletRequest request) {
		int result = CookieTool.deleteCookie(response,request);
		if(result == 1){
			return "redirect:"+loginInterface + "&acttype=" + GlobalConstants.ACT_HZB + "&inner=0" ;
		} else {
			return "zhdj/zhdj.html" ;
		}
	}


}
