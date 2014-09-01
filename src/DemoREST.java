import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

@WebServlet(urlPatterns = { "/DemoREST" })
public class DemoREST extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";

	private void showUsers(String instanceUrl, String accessToken,
			PrintWriter writer) throws ServletException, IOException {
		HttpClient httpclient = new DefaultHttpClient();

		writer.write("<br>-----show Users & edit form------<br>");
		
		// set the SOQL as a query param
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		params.add(new BasicNameValuePair("q",
				"SELECT FirstName, LastName, Id, Email from User LIMIT 100"));

		HttpGet get = new HttpGet(instanceUrl + "/services/data/v20.0/query?"
				+ URLEncodedUtils.format(params, "UTF-8"));

		// set the token in the header
		get.addHeader("Authorization", "OAuth " + accessToken);

		HttpResponse getResponse = httpclient.execute(get);
		if (getResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			// Now lets use the standard java json classes to work with the
			// results
			try {
				JSONObject response = new JSONObject(new JSONTokener(
						EntityUtils.toString(getResponse.getEntity(), "UTF-8")));
				System.out.println("Query response: " + response.toString(2));

				// writer.write(response.getString("totalSize")
				// + " record(s) returned\n\n");

				JSONArray results = response.getJSONArray("records");

				// null check
				for (int i = 0; i < results.length(); i++) {
					String firstName, lastName;
					try {
						firstName = results.getJSONObject(i).getString(
								"FirstName");
					} catch (JSONException e) {
						firstName = " ";
					}
					try {
						lastName = results.getJSONObject(i).getString(
								"LastName");
					} catch (JSONException e) {
						lastName = " ";
					}

					writer.write("id="
							+ results.getJSONObject(i).getString("Id")
							+ "<br>");
					writer.write("姓：" + lastName + ", 名：" + firstName + ", メールアドレス："
							+ results.getJSONObject(i).getString("Email")
							+ "<br>");
					String form = "<form method='POST' action='./DemoREST'>"
							+ "姓：<input type='text' name='LastName' value='"
							+ lastName + "'></input>"
							+ "名：<input type='text' name='FirstName' value='"
							+ firstName + "'></input>"
							+ "<input type='hidden' name='Id' value='"
							+ results.getJSONObject(i).getString("Id")
							+ "'></input>" + "<input type='submit'></input>"
							+ "</form>";
					writer.write(form);

				}
				writer.write("<br>");

			} catch (JSONException e) {
				getResponse.getEntity().getContent().close();
				e.printStackTrace();
				throw new ServletException(e);
			}
		}

		getResponse.getEntity().getContent().close();
	}

	private void updateUser(String userId, String newFirstName,
			String newLastName, String accessToken, String instanceUrl,
			PrintWriter writer) throws ServletException, IOException {

		HttpClient httpclient = new DefaultHttpClient();

		JSONObject update = new JSONObject();
		try {
			update.put("FirstName", newFirstName);
			update.put("LastName", newLastName);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new ServletException(e);
		}

		HttpPost patch = new HttpPost(instanceUrl
				+ "/services/data/v20.0/sobjects/User/" + userId) {
			@Override
			public String getMethod() {
				return "PATCH";
			}
		};

		patch.addHeader("Authorization", "OAuth " + accessToken);
		StringEntity stringEntity = new StringEntity(update.toString(), "UTF-8");
		stringEntity.setContentType("application/json");
		patch.setEntity(stringEntity);

		writer.write("Posting.....<br>");
		HttpResponse postResponse = httpclient.execute(patch);

		writer.write("HTTP status "
				+ postResponse.getStatusLine().getStatusCode()
				+ " updating account " + userId + "\n\n");

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html; charset=UTF-8");
		PrintWriter writer = response.getWriter();

		String accessToken = (String) request.getSession().getAttribute(
				ACCESS_TOKEN);

		String instanceUrl = (String) request.getSession().getAttribute(
				INSTANCE_URL);

		if (accessToken == null) {
			writer.write("Error - no access token");
			return;
		}

		writer.write("We have an access token:" + accessToken + "\n"
				+ "Using instance " + instanceUrl + "\n\n");

		showUsers(instanceUrl, accessToken, writer);

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		request.setCharacterEncoding("UTF-8");
		response.setContentType("text/html; charset=UTF-8");

		PrintWriter writer = response.getWriter();

		String accessToken = (String) request.getSession().getAttribute(
				ACCESS_TOKEN);

		String instanceUrl = (String) request.getSession().getAttribute(
				INSTANCE_URL);

		if (accessToken == null) {
			writer.write("Error - no access token");
			return;
		}

		String firstName = request.getParameter("FirstName");
		String lastName = request.getParameter("LastName");
		String userId = request.getParameter("Id");

		writer.write("PostedData:" + firstName + lastName + userId + "<br>");

		try {
			updateUser(userId, firstName, lastName, accessToken, instanceUrl,
					writer);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//writer.write("We have an access token:" + accessToken + "\n"
		//		+ "Using instance " + instanceUrl + "\n\n");

		showUsers(instanceUrl, accessToken, writer);
	}
}
