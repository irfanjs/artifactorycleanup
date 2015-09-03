package org.jenkinsci.plugins.artifactorydiskcleanup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ArtifactoryDiskClean
{

  /**
   * @param <T>
   * @param args
   * @throws ParseException
   * @throws IOException
   * @throws JSONException
   */
  public static void main(String[] args) throws ParseException, JSONException, IOException
  {

    // if (args.length != 3)
    // {
    // System.out.println("Usage: java -jar json-jar-with-dependencies.jar <<repo name>> <<from date>> <<to date>>");
    // return;
    // }

    // String reponanme = args[0];
    String reponanme = "scsem-yum";
    String fromdate = "01-may-2015";
    // String fromdate = args[1];
    String todate = "30-may-2015";
    // String todate = args[2];

    SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
    Date frmdate = formatter.parse(fromdate);
    Date tdate = formatter.parse(todate);

    long millisecondsfrm = frmdate.getTime();
    long millisecondsto = tdate.getTime();

    System.out.println("from date in long is :" + millisecondsfrm);
    System.out.println("to date in long is : " + millisecondsto);

    String uri = "http://qalabs-artifactory.usccqa.qalabs.symantec.com/artifactory/api/search/creation?from=" + millisecondsfrm
        + "&to=" + millisecondsto + "&repos=" + reponanme;
    System.out.println(uri);

    ArtifactoryDiskClean a = new ArtifactoryDiskClean();
    ResponseEntity<String> b = a.getRequest(uri);
    HttpStatus status = b.getStatusCode();
    String finalsttaus = status.toString();
    System.out.println("status code is : " + finalsttaus);

    if (finalsttaus.equals("200"))
    {
      System.out.println("content is good to proceed");
      String data = b.getBody();
      Integer size = data.length();
      System.out.println("size:" + size);
      final JSONObject obj = new JSONObject(data);
      final JSONArray geodata = obj.getJSONArray("results");
      final int n = geodata.length();
      for (int i = 0; i < n; ++i)
      {

        final JSONObject artifact = geodata.getJSONObject(i);
        System.out.println(artifact.getString("uri"));

        String mod = artifact.getString("uri").replaceAll("/api", "");
        String mod1 = mod.replaceAll("/storage", "");

        System.out.println("modified version is : " + mod1);
        ResponseEntity<String> c = a.delRequest(mod1);
        HttpStatus delstatus = c.getStatusCode();
        System.out.println("the http sttaus code for artifact " + mod1 + " is : " + delstatus);

      }

    }
    else
    {
      System.out.println("seems some issue in http request");
      return;

    }
  }

  static HttpHeaders createHeaders(final String username, final String password)
  {
    return new HttpHeaders()
    {
      {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
            auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);
        set("Authorization", authHeader);
      }
    };
  }

  public <T> ResponseEntity<String> getRequest(final String url)
  {
    final RestTemplate restTemplate = new RestTemplate();
    final ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<T>(createHeaders("scsem.ci", "scsem.ci")), String.class);
    return response;
  }

  public <T> ResponseEntity<String> delRequest(final String url)
  {
    final RestTemplate restTemplate = new RestTemplate();
    final ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<T>(createHeaders("scsem.ci", "scsem.ci")), String.class);
    return response;
  }
}
