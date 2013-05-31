/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wfs.servlets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;


/**
 * Simple tester for WFS post requests. Can be called two ways. If called with
 * no parameters, it displays the form, otherwise it displays the result page.
 *
 * @author Doug Cates: Moxi Media Inc.
 * @version 1.0
 */
public class TestWfsPost extends HttpServlet {
    /**
     * Initializes the servlet.
     *
     * @param config DOCUMENT ME!
     *
     * @throws ServletException DOCUMENT ME!
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Destroys the servlet.
     */
    public void destroy() {
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws ServletException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws ServletException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return DOCUMENT ME!
     */
    public String getServletInfo() {
        return "Tests a WFS post request using a form entry.";
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws ServletException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String requestString = request.getParameter("body");
        String urlString = request.getParameter("url");
        boolean doGet = (requestString == null) || requestString.trim().equals("");

        if ((urlString == null)) {
            PrintWriter out = response.getWriter();
            StringBuffer urlInfo = request.getRequestURL();

            if (urlInfo.indexOf("?") != -1) {
                urlInfo.delete(urlInfo.indexOf("?"), urlInfo.length());
            }

            String geoserverUrl = urlInfo.substring(0, urlInfo.indexOf("/", 8))
                + request.getContextPath();
            response.setContentType("text/html");
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>TestWfsPost</title>");
            out.println("</head>");
            out.println("<script language=\"JavaScript\">");
            out.println("function doNothing() {");
            out.println("}");
            out.println("function sendRequest() {");
            out.println("  if (checkURL()==true) {");
            out.print("    document.frm.action = \"");
            out.print(urlInfo.toString());
            out.print("\";\n");
            out.println("    document.frm.target = \"_blank\";");
            out.println("    document.frm.submit();");
            out.println("  }");
            out.println("}");
            out.println("function checkURL() {");
            out.println("  if (document.frm.url.value==\"\") {");
            out.println("    alert(\"Please give URL before you sumbit this form!\");");
            out.println("    return false;");
            out.println("  } else {");
            out.println("    return true;");
            out.println("  }");
            out.println("}");
            out.println("function clearRequest() {");
            out.println("document.frm.body.value = \"\";");
            out.println("}");
            out.println("</script>");
            out.println("<body>");
            out.println("<form name=\"frm\" action=\"JavaScript:doNothing()\" method=\"POST\">");
            out.println("<table align=\"center\" cellspacing=\"2\" cellpadding=\"2\" border=\"0\">");
            out.println("<tr>");
            out.println("<td><b>URL:</b></td>");
            out.print("<td><input name=\"url\" value=\"");
            out.print(geoserverUrl);
            out.print("/wfs/GetFeature\" size=\"70\" MAXLENGTH=\"100\"/></td>\n");
            out.println("</tr>");
            out.println("<tr>");
            out.println("<td><b>Request:</b></td>");
            out.println("<td><textarea cols=\"60\" rows=\"24\" name=\"body\"></textarea></td>");
            out.println("</tr>");
            out.println("</table>");
            out.println("<table align=\"center\">");
            out.println("<tr>");
            out.println(
                "<td><input type=\"button\" value=\"Clear\" onclick=\"clearRequest()\"></td>");
            out.println(
                "<td><input type=\"button\" value=\"Submit\" onclick=\"sendRequest()\"></td>");
            out.println("<td></td>");
            out.println("</tr>");
            out.println("</table>");
            out.println("</form>");
            out.println("</body>");
            out.println("</html>");
            out.close();
        } else {
            response.setContentType("application/xml");

            BufferedReader xmlIn = null;
            PrintWriter xmlOut = null;
            StringBuffer sbf = new StringBuffer();
            String resp = null;

            try {
                URL u = new URL(urlString);
                java.net.HttpURLConnection acon = (java.net.HttpURLConnection) u.openConnection();
                acon.setAllowUserInteraction(false);

                if (!doGet) {
                    //System.out.println("set to post");
                    acon.setRequestMethod("POST");
                    acon.setRequestProperty("Content-Type", "application/xml");
                } else {
                    //System.out.println("set to get");
                    acon.setRequestMethod("GET");
                }

                acon.setDoOutput(true);
                acon.setDoInput(true);
                acon.setUseCaches(false);

                //SISfixed - if there was authentication info in the request,
                //           Pass it along the way to the target URL
                //DJB: applied patch in GEOS-335
                String authHeader = request.getHeader("Authorization");

                String username = request.getParameter("username");

                if ((username != null) && !username.trim().equals("")) {
                    String password = request.getParameter("password");
                    String up = username + ":" + password;
                    byte[] encoded = Base64.encodeBase64(up.getBytes());
                    authHeader = "Basic " + new String(encoded);
                }

                if (authHeader != null) {
                    acon.setRequestProperty("Authorization", authHeader);
                }

                if (!doGet) {
                    xmlOut = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(acon.getOutputStream())));
                    xmlOut = new java.io.PrintWriter(acon.getOutputStream());

                    xmlOut.write(requestString);
                    xmlOut.flush();
                }

                // Above 400 they're all error codes, see:
                // http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
                if (acon.getResponseCode() >= 400) {
                    PrintWriter out = response.getWriter();
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<servlet-exception>");
                    out.println("HTTP response: " + acon.getResponseCode() + "\n"
                        + URLDecoder.decode(acon.getResponseMessage(), "UTF-8"));
                    out.println("</servlet-exception>");
                    out.close();
                } else {
                    // xmlIn = new BufferedReader(new InputStreamReader(
                    // acon.getInputStream()));
                    // String line;

                    // System.out.println("got encoding from acon: "
                    // + acon.getContentType());
                    response.setContentType(acon.getContentType());
                    response.setHeader("Content-disposition",
                        acon.getHeaderField("Content-disposition"));

                    OutputStream output = response.getOutputStream();
                    int c;
                    InputStream in = acon.getInputStream();

                    while ((c = in.read()) != -1)
                        output.write(c);

                    in.close();
                    output.close();
                }

                //while ((line = xmlIn.readLine()) != null) {
                //    out.print(line);
                //}
            } catch (Exception e) {
                PrintWriter out = response.getWriter();
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                out.println("<servlet-exception>");
                out.println(e.toString());
                out.println("</servlet-exception>");
                out.close();
            } finally {
                try {
                    if (xmlIn != null) {
                        xmlIn.close();
                    }
                } catch (Exception e1) {
                    PrintWriter out = response.getWriter();
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<servlet-exception>");
                    out.println(e1.toString());
                    out.println("</servlet-exception>");
                    out.close();
                }

                try {
                    if (xmlOut != null) {
                        xmlOut.close();
                    }
                } catch (Exception e2) {
                    PrintWriter out = response.getWriter();
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<servlet-exception>");
                    out.println(e2.toString());
                    out.println("</servlet-exception>");
                    out.close();
                }
            }
        }
    }
}
