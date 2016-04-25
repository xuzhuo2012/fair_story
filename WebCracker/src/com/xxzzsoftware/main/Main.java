package com.xxzzsoftware.main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.hc.client5.http.impl.sync.HttpClientBuilder;
import org.apache.hc.client5.http.methods.HttpGet;
import org.apache.hc.client5.http.methods.HttpPost;
import org.apache.hc.client5.http.sync.HttpClient;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.entity.StringEntity;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import com.google.gson.Gson;
import com.xxzzsoftware.model.Article;
import com.xxzzsoftware.model.Category;

public class Main {

	// 格林童话
//	private static String BASEURL = "http://www.5068.com/gs/gl/";
//	private static String FILE = "file/article_gl.json";

	// 安徒生童话
	 private static String BASEURL = "http://www.5068.com/gs/ats/";
	 private static String FILE = "file/article_ats.json";

	// 一千零一夜
	// private static String BASEURL = "http://www.5068.com/gs/yqlyy/";
	// private static String FILE = "file/article_yqlyy.json";

	public static void main(String[] args) {

		// String url = "http://www.5068.com/gs/gl/";

		try {
			String url = BASEURL;
			saveToFile("[\r\n");
			articleCracker(url);
			saveToFile("\r\n]");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPatherException e) {
			e.printStackTrace();
		}
	}

	public static void articleCracker(String url) throws IOException, XPatherException {

		HttpClient httpclient = HttpClientBuilder.create().build();

		HttpResponse response = httpclient.execute(new HttpGet(url));
		InputStream entityContent = response.getEntity().getContent();

		HtmlCleaner hc = new HtmlCleaner();
		TagNode node = hc.clean(entityContent, "gb2312");

		// 分页数据
		Object[] nextPage = node.evaluateXPath("//*[@id=\"con\"]//div[@class=\"list-page\"]//li");

		boolean hasNextPage = false;
		String nextPageUrl = null;
		if (nextPage != null && nextPage.length > 0) {
			for (Object o : nextPage) {
				TagNode n = (TagNode) o;
				if (n.getText().toString().equalsIgnoreCase("下一页")) {
					hasNextPage = true;
					nextPageUrl = BASEURL + (String) n.evaluateXPath("//a/@href")[0];
				}
			}
		}

		// 文章内容
		Object[] obj = node.evaluateXPath("//*[@id=\"con\"]/div[4]/div[5]/div[2]/div[2]/div/div[1]/ul/li");

		if (obj != null && obj.length > 0) {

			for (int i = 0; i < obj.length; i++) {
				TagNode tntr = (TagNode) obj[i];
				String title = tntr.getText().toString();
				String imgUrl = (String) tntr.evaluateXPath("//img/@original")[0];
				String contentUrl = (String) tntr.evaluateXPath("//a/@href")[0];
				String content = contentCracker(contentUrl);

				Article article = new Article();
				article.setTitle(title);
				article.setImgUrl(imgUrl);
				article.setContent(content);
				// insertArticle(article);
				saveToFile(new Gson().toJson(article));
				if (i == obj.length - 1 && !hasNextPage) {
					System.out.println("最后一条");
				} else {
					saveToFile(",\r\n");
				}

				System.out.println("Raw : " + new Gson().toJson(article));
			}
		}

		// 爬取下一页
		System.out.println(">>>>下一页链接>>>>>:" + nextPageUrl);
		if (hasNextPage) {
			articleCracker(nextPageUrl);
		}

	}

	public static String contentCracker(String url) throws IOException, XPatherException {

		HttpClient httpclient = HttpClientBuilder.create().build();

		HttpResponse response = httpclient.execute(new HttpGet(url));
		InputStream content = response.getEntity().getContent();

		HtmlCleaner hc = new HtmlCleaner();
		TagNode node = hc.clean(content, "gb2312");

		String xpath = "//*[@id=\"con\"]/div[5]/div[9]/div[2]/div/div[@class=\"rg_txt\"]";

		Object[] obj = node.evaluateXPath(xpath);

		if (obj != null && obj.length > 0) {
			TagNode tntr = (TagNode) obj[0];
			// 去除广告内容
			TagNode findElementByAttValue = tntr.findElementByAttValue("class", "nr_gg250", true, false);
			findElementByAttValue.removeFromTree();
			String innerHtml = hc.getInnerHtml(tntr);
			return innerHtml;
		}

		return "";
	}

	public static void categoryCracker() {
		try {

			HttpClient httpclient = HttpClientBuilder.create().build();

			HttpResponse response = httpclient.execute(new HttpGet("http://www.5068.com/gs"));
			InputStream content = response.getEntity().getContent();

			HtmlCleaner hc = new HtmlCleaner();
			TagNode node = hc.clean(content, "gb2312");

			Object[] obj = node.evaluateXPath("//*[@id=\"con\"]/div[4]/div[4]/div[1]/div[1]/div[2]/div/ul/li");

			if (obj != null && obj.length > 0) {

				for (int i = 0; i < obj.length; i++) {
					TagNode tntr = (TagNode) obj[i];

					String name = tntr.getText().toString();
					String url = (String) tntr.evaluateXPath("//a/@href")[0];

					Category category = new Category();
					category.setId(i);
					category.setName(name);
					category.setUrl(url);

					System.out.println("Raw : " + new Gson().toJson(category));
					// insertCategory(category);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void insertArticle(Article article) throws IOException {

		HttpClient httpclient = HttpClientBuilder.create().build();

		String url = "https://api.bmob.cn/1/classes/article";
		HttpPost post = new HttpPost(url);
		post.setHeader("X-Bmob-Application-Id", "14b84f54e467322cd9f0fb35e7a20a07");
		post.setHeader("X-Bmob-REST-API-Key", "e5095d93ce3f8699705f6456daaab43f");
		post.setHeader("Content-Type", "application/json");

		System.out.println("Raw : " + new Gson().toJson(article));

		StringEntity stringEntity = new StringEntity(new Gson().toJson(article));
		stringEntity.setContentType("utf-8");
		post.setEntity(stringEntity);
		HttpResponse res = httpclient.execute(post);
		String streamToString = streamToString(res.getEntity().getContent());

		System.out.println("Code:" + res.getCode());
		System.out.println("Response:" + streamToString);

	}

	public static void insertCategory(Category category) throws IOException {

		HttpClient httpclient = HttpClientBuilder.create().build();

		String url = "https://api.bmob.cn/1/classes/category";
		HttpPost post = new HttpPost(url);
		post.setHeader("X-Bmob-Application-Id", "14b84f54e467322cd9f0fb35e7a20a07");
		post.setHeader("X-Bmob-REST-API-Key", "e5095d93ce3f8699705f6456daaab43f");
		post.setHeader("Content-Type", "application/json");

		System.out.println("Raw : " + new Gson().toJson(category));

		StringEntity stringEntity = new StringEntity(new Gson().toJson(category));
		stringEntity.setContentType("utf-8");
		post.setEntity(stringEntity);
		HttpResponse res = httpclient.execute(post);
		String streamToString = streamToString(res.getEntity().getContent());

		System.out.println("Code:" + res.getCode());
		System.out.println("Response:" + streamToString);

	}

	public static String streamToString(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedInputStream bis = new BufferedInputStream(is);
		byte[] b = new byte[1024];
		int c = 0;
		while ((c = bis.read(b)) != -1) {
			sb.append(new String(b, 0, c));
		}
		return sb.toString();
	}

	public static void saveToFile(String msg) throws IOException {

		FileOutputStream fos = new FileOutputStream(new File(FILE), true);
		fos.write(msg.getBytes(), 0, msg.getBytes().length);
		fos.flush();
		fos.close();
	}

}
