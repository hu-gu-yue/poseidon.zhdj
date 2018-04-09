package poseidon.zhdj.controller.synchronize;

import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import poseidon.zhdj.controller.BaseController;
import poseidon.zhdj.db.global.GlobalConstants;
import poseidon.zhdj.db.video.model.UserModel;
import poseidon.zhdj.db.video.model.VideoModel;
import poseidon.zhdj.db.video.service.UserService;
import poseidon.lib.core.tool.date.DateTool;
import poseidon.lib.core.tool.string.StringTool;
import poseidon.lib.core.tool.xml.XmlUtil;
import poseidon.zhdj.db.video.service.VideoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.*;

@Controller
public class ZiyuanSyn extends BaseController{
	private final static Logger log = LoggerFactory.getLogger(ZiyuanSyn.class);
	@Autowired
	VideoService videoService;
	@Autowired
	UserService userService;

	@RequestMapping(value = "/ziyuansyn",method= RequestMethod.POST)
	public void ziyuansyn(HttpServletRequest request, HttpServletResponse response) throws Exception {

		log.debug("--------------资源同步开始----------------");

		Enumeration<String> headers = request.getHeaderNames();
		Map<String, String> hm = new HashMap<String, String>();
		while(headers.hasMoreElements()){
			;String name = headers.nextElement();
			hm.put(name, request.getHeader(name));
		}
		log.debug("--------------header信息----------------" + hm);

		String target = request.getRequestURI();

		//verify request
		String cert = request.getHeader("x-mns-signing-cert-url");
		if (cert.isEmpty()) {
			log.warn("SigningCertURL empty");
			response.setStatus(HttpStatus.SC_BAD_REQUEST);
			return;
		}
		cert = new String(Base64.decodeBase64(cert));
		log.debug("SigningCertURL:\t" + cert);


		if (!authenticate("POST", target, hm, cert)) {
			log.warn("authenticate fail");
			response.setStatus(HttpStatus.SC_BAD_REQUEST);
			return;
		}

		StringBuffer sb = new StringBuffer();
		BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream(), "utf-8"));
		String str = null;
		while ((str = in.readLine()) != null) {
			sb.append(str);
		}
		in.close();
		log.debug("--------------读取XML报文结束-------------------" + sb.toString());
		String TopicOwner = XmlUtil.getElementValue(sb.toString(), "TopicOwner");//被订阅主题的拥有者
		String TopicName = XmlUtil.getElementValue(sb.toString(), "TopicName");//被订阅主题的名称
		String Subscriber = XmlUtil.getElementValue(sb.toString(), "Subscriber");//订阅者
		String SubscriptionName = XmlUtil.getElementValue(sb.toString(), "SubscriptionName");//订阅名称
		String MessageId = XmlUtil.getElementValue(sb.toString(), "MessageId");//消息编号
		String Message = XmlUtil.getElementValue(sb.toString(), "Message");//消息正文
		String MessageMD5 = XmlUtil.getElementValue(sb.toString(), "MessageMD5");//消息的 MD5 值
		String PublishTime = XmlUtil.getElementValue(sb.toString(), "PublishTime");//消息的发布时间，从 1970-1-1 00:00:00 000 到消息发布时的毫秒值
		String MessageTag = XmlUtil.getElementValue(sb.toString(), "MessageTag");//消息标签（用于消息过滤）
		log.debug("-------------------从XML中获取的message = " + Message);

		log.debug("TopicOwner = " + TopicOwner + ",TopicName=" + TopicName+ ",Subscriber=" + Subscriber+ ",SubscriptionName=" + SubscriptionName+ ",MessageId=" + MessageId+ ",MessageMD5=" + MessageMD5
				+ ",PublishTime=" + PublishTime);

		if(!"ziyuansyn".equals(MessageTag)){
			response.setStatus(HttpStatus.SC_OK);
			log.debug("-------------------丢弃无效请求(MessageTag != ziyuansyn)   MessageTag = " + MessageTag);
             return;
		}

		//逐一取出json消息串中的内容
		JSONObject jsonObject= JSONObject.fromObject(Message);
		String action = jsonObject.getString("action");
		String fileId = jsonObject.getString("fileId");
		String broadId = jsonObject.getString("broadId");

		VideoModel record = new VideoModel();
		record.setmTime(DateTool.now());
       //判断是青创精还是和主播的资源？
		if("132".equals(broadId)){ //青创精板块ID：132
			record.setAct(GlobalConstants.ACT_QCJ);
		} else if("134".equals(broadId)){ //和主播板块ID：134
			record.setAct(GlobalConstants.ACT_HZB);
		} else {
			record.setAct(GlobalConstants.ACT_HZB);
		}

		if(StringTool.isNotEmpty(fileId)){
			record.setZyId(Long.valueOf(fileId));
		}

		if(StringTool.isNotEmpty(action) && "delete".equals(action)){  //如果同步删除资源，则根据板块ID跟资源资源id删除
			log.error("删除的资源编号：" + record.getZyId());
			record.setStatus(1);
//			videoService.deleteByActZyId(record.getAct(), record.getZyId());
			videoService.insert(record);
		} else{ //判断是新增或者修改资源
			String fileName = jsonObject.getString("fileName");
			String suffix = jsonObject.getString("suffix");
			String playPath = jsonObject.getString("playPath");
			String imgPath = jsonObject.getString("imgPath");
			String sharetopic = jsonObject.getString("sharetopic");
			String summary = jsonObject.getString("summary");
			String userName = jsonObject.getString("userName");
			String avatar = jsonObject.getString("avatar");
			String userId = jsonObject.getString("userId");
			String deptName = jsonObject.getString("deptName");

			String unitName = jsonObject.getString("unitName");
			String datetime = jsonObject.getString("datetime");

			String stags = jsonObject.getString("stags");//和主播模块才有的视频类型
			String docType = jsonObject.getString("bigType");//文档类型：media,document
			String sign = jsonObject.getString("sign");//资源签名，数据交互用来验证
			try{
				if(StringTool.isNotEmpty(fileName)){
					record.setTitle(fileName);
				}
				if(StringTool.isNotEmpty(suffix)){
					record.setZyType(suffix);
				}
				if(StringTool.isNotEmpty(playPath)){
					record.setUrl(convertURL(playPath));
				}
				if(StringTool.isNotEmpty(imgPath)){
					record.setVideoImg(convertURL(imgPath));
				}
				if(StringTool.isNotEmpty(sharetopic)){
					record.setDesc(sharetopic);
				}
				if(StringTool.isNotEmpty(userId)){
					record.setUid(userId);
				}
				if(StringTool.isNotEmpty(userName)){
					record.setUserName(userName);
				}
//				if(StringTool.isNotEmpty(datetime)){
//					SimpleDateFormat format =   new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
//					Long time=new Long(datetime);
//					String d = format.format(time);
//					Date date = format.parse(d);
//					record.setCTime(DateTool.now());
//				}
				if(StringTool.isNotEmpty(stags)){
					if(stags.indexOf("生活") >= 0){
						record.setVideoType(GlobalConstants.VideoType.LIFE);
					} else if(stags.indexOf("工作") >= 0){
						record.setVideoType(GlobalConstants.VideoType.WORK);
					}
				}
				//1：文档，2：视频
				if(StringTool.isNotEmpty(docType)){
					if(docType.indexOf("media") >= 0){
						record.setDocType(GlobalConstants.VideoDocumentType.VIDEO);
					} else if(docType.indexOf("document") >= 0){
						record.setDocType(GlobalConstants.VideoDocumentType.DOCUMENT);
					} else if(docType.indexOf("picture") >= 0){
						record.setDocType(GlobalConstants.VideoDocumentType.IMG);
					} else if(docType.indexOf("music") >= 0){
						record.setDocType(GlobalConstants.VideoDocumentType.MUSIC);
					}
				}
				//资源签名，数据交互用来验证
				if(StringTool.isNotEmpty(sign)){
					record.setSign(sign);
				}
			}
			catch(Exception ex){
				ex.printStackTrace();
				log.error("-------------------同步资源zyId = " + fileId + " 异常:"  + ex.getMessage());
			}

			videoService.insert(record);  //资源入表

			UserModel userModel = new UserModel();
			if(StringTool.isNotEmpty(userId)){
				userModel.setUid(userId);
			}
			if(StringTool.isNotEmpty(avatar)){
				userModel.setHeadImg(avatar);
			}
			if(StringTool.isNotEmpty(userName)){
				userModel.setUserName(userName);
			}
			if(StringTool.isNotEmpty(deptName)){
				userModel.setDepartment(deptName);
			}
			if(StringTool.isNotEmpty(unitName)){
				userModel.setUnitName(unitName);
			}
			userModel.setLoginTime(DateTool.now());
			userService.saveUser(userModel); //用户信息入表
		}
		/**
		 * 正常处理通知消息，返回 204；
		 请求签名验证不通过，返回 403；
		 其他任何错误，返回 500。
		 */
		response.setStatus(HttpStatus.SC_OK);
		log.debug("--------------资源同步结束----------------");
	}

	private String convertURL(String url){
		StringBuilder sb = new StringBuilder();
		try{
			URL urlObject = new URL(url);
			URLConnection uc = urlObject.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			String str = null;
			while ((str = in.readLine()) != null) {
				sb.append(str);
			}
			in.close();
			return sb.toString();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * check if this request comes from MNS Server
	 * @param method, http method
	 * @param uri, http uri
	 * @param headers, http headers
	 * @param cert, cert url
	 * @return true if verify pass
	 */
	private Boolean authenticate(String method, String uri, Map<String, String> headers, String cert) {
		String str2sign = getSignStr(method, uri, headers);
		//System.out.println(str2sign);
		String signature = headers.get("authorization");
		byte[] decodedSign = Base64.decodeBase64(signature);
		//get cert, and verify this request with this cert
		try {
			//String cert = "http://mnstest.oss-cn-hangzhou.aliyuncs.com/x509_public_certificate.pem";
			URL url = new URL(cert);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			DataInputStream in = new DataInputStream(conn.getInputStream());
			CertificateFactory cf = CertificateFactory.getInstance("X.509");

			Certificate c = cf.generateCertificate(in);
			PublicKey pk = c.getPublicKey();

			java.security.Signature signetcheck = java.security.Signature.getInstance("SHA1withRSA");
			signetcheck.initVerify(pk);
			signetcheck.update(str2sign.getBytes());
			Boolean res = signetcheck.verify(decodedSign);
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			log.warn("authenticate fail, " + e.getMessage());
			return false;
		}
	}

	/**
	 * build string for sign
	 * @param method, http method
	 * @param uri, http uri
	 * @param headers, http headers
	 * @return String fro sign
	 */
	private String getSignStr(String method, String uri, Map<String, String> headers) {
		StringBuilder sb = new StringBuilder();
		sb.append(method);
		sb.append("\n");
		sb.append(safeGetHeader(headers, "content-md5"));
		sb.append("\n");
		sb.append(safeGetHeader(headers, "content-type"));
		sb.append("\n");
		sb.append(safeGetHeader(headers, "date"));
		sb.append("\n");

		List<String> tmp = new ArrayList<String>();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			if (entry.getKey().startsWith("x-mns-"))
				tmp.add(entry.getKey() + ":" + entry.getValue());
		}
		Collections.sort(tmp);

		for (String kv : tmp) {
			sb.append(kv);
			sb.append("\n");
		}

		sb.append(uri);
		return sb.toString();
	}

	private String safeGetHeader(Map<String, String> headers, String name) {
		if (headers.containsKey(name))
			return headers.get(name);
		else
			return "";
	}



}
