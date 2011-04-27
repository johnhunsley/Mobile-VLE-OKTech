package com.mobilevle.oktech;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Date;
import java.io.IOException;

import org.ksoap2.transport.HttpTransportSE;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.xmlpull.v1.XmlPullParserException;
import android.util.Log;
import android.content.Context;
import com.mobilevle.core.*;
import com.mobilevle.core.moodle.*;
import com.mobilevle.oktech.session.Session;
import com.mobilevle.oktech.session.SessionDAOSQLiteImpl;
import com.mobilevle.oktech.session.SessionDAO;

/**
 *
 * <p>A Moodle OK Tech Web Service client implementation of the {@link MoodleVLEHandler} </p>
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * </p>
 *
 * @author johnhunsley
 *         Date: 09-Nov-2010
 *         Time: 15:25:13
 */
public class VLEHandlerOKTechImpl implements MoodleVLEHandler {
    private static final String LOGIN_METHOD_NAME = "login";
    private static final String GET_USER_ID_LOGIN_METHOD_NAME = "get_my_id";
    private static final String GET_MY_COURSES_METHOD_NAME = "get_my_courses";
    private static final String GET_COURSE_INSTANCES = "get_instances_bytype";
    private static final String GET_STUDENTS_METHOD_NAME = "get_students";
    private static final String GET_TEACHERS_METHOD_NAME = "get_teachers";
    private static final String GET_MESSAGES_METHOD_NAME = "get_messages";
    private static final String GET_MESSAGES_HISTORY_METHOD_NAME = "get_messages_history";
    private String url;
    private String namespace;
    private SessionDAO sessionDAO;
    private static final String SEND_MESSAGE_METHOD_NAME = "message_send";
    private static final String GET_USER_METHOD_NAME = "get_user";
    private static final String NO_MATCH = "[[ws_nomatch]]";
    private static final String NO_MESSAGES = "[[nomessages]]";

    /**
     * <p>Add an instance of this class to the {@link VLEHandlerFactory}</p>
     */
    static {
        VLEHandlerFactory factory = VLEHandlerFactory.getInstance();

        try {
            factory.addVLEHandler(new VLEHandlerOKTechImpl());

        } catch (MobileVLECoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param params
     */
    public void setParams(Map<String, String> params) {
        this.url = params.get("url");
        this.namespace = params.get("namespace");
    }

    /**
     *
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     * @return namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * <p>
     *   Request -
     *
     *      <soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsdl="http://localhost/moodle/wspp/wsdl">
     *          <soapenv:Header/>
     *          <soapenv:Body>
     *             <wsdl:login soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
     *                <username xsi:type="xsd:string">username</username>
     *                <password xsi:type="xsd:string">password</password>
     *             </wsdl:login>
     *          </soapenv:Body>
     *       </soapenv:Envelope>
     *
     *   Response -
     *
     *       <SOAP-ENV:Envelope SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns1="http://localhost/moodle/wspp/wsdl" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:SOAP-ENC="http://schemas.xmlsoap.org/soap/encoding/">
     *          <SOAP-ENV:Body>
     *             <ns1:loginResponse>
     *                <return xsi:type="ns1:loginReturn">
     *                   <client xsi:type="xsd:integer">userId</client>
     *                   <sessionkey xsi:type="xsd:string">token</sessionkey>
     *                </return>
     *             </ns1:loginResponse>
     *          </SOAP-ENV:Body>
     *       </SOAP-ENV:Envelope>
     * </p>
     * @param username
     * @param password
     * @param rememberPass
     * @return
     */
    public boolean authenticate(final Context context,
                                final String username,
                                final String password,
                                final boolean rememberPass) throws InvalidSessionException {
        Log.i("VLEHandlerOKTechImpl", "authenticate()....");

        try {
            SoapObject request = new SoapObject(namespace, LOGIN_METHOD_NAME);
            request.addProperty("username", username);
            request.addProperty("password", password);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
            androidHttpTransport.call(LOGIN_METHOD_NAME, envelope);
            SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
            SoapObject loginResult = (SoapObject)resultsRequestSOAP.getProperty(0);
            String clientId = loginResult.getProperty("client").toString();
            String token = loginResult.getProperty("sessionkey").toString();
            boolean success = (token != null && token.length() > 0);

            if(success) {
                Log.i("VLEHandlerOKTechImpl", "user id = "+clientId.toString()+"\ntoken = "+token.toString());
                Session session = new Session();
                session.setUsername(username);
                session.setPassword(password);
                session.setClientId(Integer.parseInt(clientId));
                session.setToken(token);
                discoverAutneticatedUserId(session);
                persistSession(session, context, rememberPass);
            }

            return success;

        } catch (Exception e) {        //todo throw InvalidSessionException and UnknownHostException -- see I/VLEHandlerOKTechImpl(19576): org.xmlpull.v1.XmlPullParserException: unexpected type (position:END_DOCUMENT null@1:0 in java.io.InputStreamReader@46361100)
            Log.i("VLEHandlerOKTechImpl", e.toString());
            throw new InvalidSessionException(e);
        }
    }

    /**
     *
     * @param session
     * @param context
     */
    private void persistSession(Session session, Context context, final boolean rememberPass) {
        Log.i("VLEHandlerOKTechImpl", "persistSession()");
        sessionDAO = new SessionDAOSQLiteImpl(context);
        sessionDAO.saveSession(session, rememberPass);
    }

    /**
     *
     * @param session
     */
    private Session discoverAutneticatedUserId(Session session) throws InvalidSessionException {

        try {
            SoapObject request = new SoapObject(namespace, GET_USER_ID_LOGIN_METHOD_NAME);
            request.addProperty("client", Integer.toString(session.getClientId()));
            request.addProperty("sesskey", session.getToken());
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
            androidHttpTransport.call(GET_USER_ID_LOGIN_METHOD_NAME, envelope);
            SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
            String userId = resultsRequestSOAP.getProperty(0).toString();
            session.setUserId(userId);
            return session;

        } catch (IOException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);

        } catch (XmlPullParserException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);
        }
    }

    /**
     * get_my_coursesResponse{return=getCoursesReturn
     *   {courses=courseRecords
     *      {item=courseRecord
     *          {error=;
     *              id=2; category=1; sortorder=2000; password=; fullname=Java Programming; shortname=JP100; idnumber=;
     *              summary= A course all about Java ; format=weeks; showgrades=1; newsitems=5; teacher=Teacher;
     *              teachers=Teachers; student=Student; students=Students; guest=0; startdate=1288220400; enrolperiod=0;
     *              numsections=10; marker=0; maxbytes=2097152; visible=1; hiddensections=0; groupmode=2; groupmodeforce=0;
     *              lang=; theme=; cost=; timecreated=1288184867; timemodified=1288912682; metacourse=0;myrole=1;
     *          };
     *      };
     *   };
     *}
     * @return  List courseNames
     * @throws InvalidSessionException
     */
    public List<Course> getCourses() throws InvalidSessionException {
        if(sessionDAO == null) throw new InvalidSessionException();

        try {
            Session session = sessionDAO.loadSession();
            SoapObject request = new SoapObject(namespace, GET_MY_COURSES_METHOD_NAME);
            request.addProperty("client", session.getClientId());
            request.addProperty("sesskey", session.getToken());
            request.addProperty("uid", session.getUserId());
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
            androidHttpTransport.call(GET_MY_COURSES_METHOD_NAME, envelope);
            SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
            SoapObject coursesResult = (SoapObject)resultsRequestSOAP.getProperty(0);
            SoapObject coursesRecords = (SoapObject)coursesResult.getProperty(0);
            int coursesCount = coursesRecords.getPropertyCount();
            List<Course> courses = new ArrayList<Course>();

            for(int i = 0; i < coursesCount; i++) {
                SoapObject courseRecord = (SoapObject)coursesRecords.getProperty(i);
                Course course = new Course();
                course.setId(Integer.parseInt(courseRecord.getProperty("id").toString()));
                course.setCourseId(courseRecord.getProperty("shortname").toString());
                course.setName(courseRecord.getProperty("fullname").toString());
                courses.add(course);
            }

            return courses;

        } catch (IOException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);

        } catch (XmlPullParserException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);
        }
    }

    /**
     *
     * @param courseId
     * @return
     * @throws InvalidSessionException
     */
    public List<Activity> getCourseActivities(final int courseId) throws InvalidSessionException {
        return null;
    }

    /**
     * <p>Get the {@link Activity} implementations for the given course id as
     * defined by the given {@link ActivityType}</p>
     * @param courseId
     * @param activityType
     * @return list of {@link Activity} defined by the given {@link ActivityType}
     */
    public List<Activity> getCourseActivityByType(final int courseId,
                                                  final ActivityType activityType) throws InvalidSessionException {
        if(sessionDAO == null) throw new InvalidSessionException();

        try {
            Session session = sessionDAO.loadSession();
            SoapObject request = new SoapObject(namespace, GET_COURSE_INSTANCES);
            request.addProperty("client", session.getClientId());
            request.addProperty("sesskey", session.getToken());
            SoapObject nested = new SoapObject(namespace, "getCoursesInput");
            nested.addProperty("item", Integer.toString(courseId));
            request.addProperty("courseids", nested);
            request.addProperty("idfield", "id");
            request.addProperty("type", activityType.label);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
            androidHttpTransport.call(GET_COURSE_INSTANCES, envelope);
            SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
            SoapObject activitiesResult = (SoapObject)resultsRequestSOAP.getProperty(0);
            SoapObject activitiesRecords = (SoapObject)activitiesResult.getProperty(0);
            int actvitiyCount = activitiesRecords.getPropertyCount();
            List<Activity> activities = new ArrayList<Activity>();

            for(int i = 0; i < actvitiyCount; i++) {

                try {
                    SoapObject activityRecord = (SoapObject)activitiesRecords.getProperty(i);
                    Activity activity = Activity.createActivitybyType(activityType);
                    activity.setId(Integer.parseInt(activityRecord.getProperty("id").toString()));
                    activity.setName(activityRecord.getProperty("name").toString());
                    activity.setSummary(activityRecord.getProperty("summary").toString());
                    activities.add(activity);

                } catch (MobileVLECoreException e) {
                    Log.e("VLEHandlerOKTechImpl", e.getMessage());
                }
            }

            return activities;

        } catch (IOException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);

        } catch (XmlPullParserException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);
        }
    }

    /**
     * <p>
     *     <soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsdl="http://localhost/moodle/wspp/wsdl">
     <soapenv:Header/>
     <soapenv:Body>
     <wsdl:get_students soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
     <client xsi:type="xsd:integer">93</client>
     <sesskey xsi:type="xsd:string">945f9f68b77071b736680e2689c93274</sesskey>
     <value xsi:type="xsd:string">2</value>
     <id xsi:type="xsd:string">id</id>
     </wsdl:get_students>
     </soapenv:Body>
     </soapenv:Envelope>


     <ns1:get_studentsResponse>
     <return xsi:type="ns1:getUsersReturn">
     <users SOAP-ENC:arrayType="ns1:userRecord[2]" xsi:type="ns1:userRecords">
     <item xsi:type="ns1:userRecord">
     <error xsi:type="xsd:string"/>
     <id xsi:type="xsd:int">3</id>
     <auth xsi:type="xsd:string"/>
     <confirmed xsi:type="xsd:int">1</confirmed>
     <policyagreed xsi:type="xsd:int">0</policyagreed>
     <deleted xsi:type="xsd:int">0</deleted>
     <username xsi:type="xsd:string">fred</username>
     <idnumber xsi:type="xsd:string"/>
     <firstname xsi:type="xsd:string">Fred</firstname>
     <lastname xsi:type="xsd:string">Bloggs</lastname>
     <email xsi:type="xsd:string">fredbloggs@mymoodle.com</email>
     <icq xsi:type="xsd:string"/>
     <emailstop xsi:type="xsd:int">0</emailstop>
     <skype xsi:type="xsd:string"/>
     <yahoo xsi:type="xsd:string"/>
     <aim xsi:type="xsd:string"/>
     <msn xsi:type="xsd:string"/>
     <phone1 xsi:type="xsd:string"/>
     <phone2 xsi:type="xsd:string"/>
     <institution xsi:type="xsd:string"/>
     <department xsi:type="xsd:string"/>
     <address xsi:type="xsd:string"/>
     <city xsi:type="xsd:string">Leeds</city>
     <country xsi:type="xsd:string">GB</country>
     <lang xsi:type="xsd:string">en_utf8</lang>
     <timezone xsi:type="xsd:int">99</timezone>
     <mnethostid xsi:type="xsd:int">1</mnethostid>
     <lastip xsi:type="xsd:string"/>
     <theme xsi:type="xsd:string"/>
     <description xsi:type="xsd:string"/>
     <role xsi:type="xsd:int">5</role>
     <profile SOAP-ENC:arrayType="ns1:profileitemRecord[0]" xsi:type="ns1:profileitemRecords"/>
     </item>
     *
     * </p>
     * @param courseId
     * @return
     * @throws InvalidSessionException
     */
    public List<User> getStudentsByCourse(final int courseId) throws InvalidSessionException {
        if(sessionDAO == null) throw new InvalidSessionException();

        return getUsersByRoleAndCourse(courseId, User.STUDENT_ROLE, GET_STUDENTS_METHOD_NAME);
    }

    /**
     * <p></p>
     * @param courseId
     * @return
     * @throws InvalidSessionException
     */
    public List<User> getCourseUsers(final int courseId) throws InvalidSessionException {
        List<User> users = getStudentsByCourse(courseId);
        users.addAll(getTeachersByCourse(courseId));
        return users;
    }

    /**
     * <p></p>
     * @param courseId
     * @param roleId
     * @param method
     * @return
     * @throws InvalidSessionException
     */
    private List<User> getUsersByRoleAndCourse(final int courseId, final int roleId, final String method) throws InvalidSessionException {
        try {
            Session session = sessionDAO.loadSession();
            SoapObject request = new SoapObject(namespace, method);
            request.addProperty("client", session.getClientId());
            request.addProperty("sesskey", session.getToken());
            request.addProperty("value", Integer.toString(courseId));
            request.addProperty("id", "id");
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
            androidHttpTransport.call(method, envelope);
            SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
            SoapObject usersResult = (SoapObject)resultsRequestSOAP.getProperty(0);
            SoapObject usersRecords = (SoapObject)usersResult.getProperty(0);
            int actvitiyCount = usersRecords.getPropertyCount();
            List<User> users = new ArrayList<User>();

            for(int i = 0; i < actvitiyCount; i++) {
                SoapObject userRecord = (SoapObject)usersRecords.getProperty(i);
                User user = new User(roleId);
                user.setId(userRecord.getProperty("id").toString());
                user.setFirstName(userRecord.getProperty("firstname").toString());
                user.setLastName(userRecord.getProperty("lastname").toString());
                user.setEmail(userRecord.getProperty("email").toString());
                user.setUsername(userRecord.getProperty("username").toString());
                users.add(user);
            }

            return users;

        } catch (IOException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);

        } catch (XmlPullParserException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);
        }
    }

    /**
     * <p>
     * <soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsdl="http://localhost/moodle/wspp/wsdl">
     <soapenv:Header/>
     <soapenv:Body>
     <wsdl:get_teachers soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
     <client xsi:type="xsd:integer">94</client>
     <sesskey xsi:type="xsd:string">e0b782906d4a6f93dd25de26fc7d4374</sesskey>
     <value xsi:type="xsd:string">2</value>
     <id xsi:type="xsd:string">id</id>
     </wsdl:get_teachers>
     </soapenv:Body>
     </soapenv:Envelope>



     <ns1:get_teachersResponse>
     <return xsi:type="ns1:getUsersReturn">
     <users SOAP-ENC:arrayType="ns1:userRecord[1]" xsi:type="ns1:userRecords">
     <item xsi:type="ns1:userRecord">
     <error xsi:type="xsd:string"/>
     <id xsi:type="xsd:int">2</id>
     <auth xsi:type="xsd:string"/>
     <confirmed xsi:type="xsd:int">1</confirmed>
     <policyagreed xsi:type="xsd:int">0</policyagreed>
     <deleted xsi:type="xsd:int">0</deleted>
     <username xsi:type="xsd:string">admin</username>
     <idnumber xsi:type="xsd:string"/>
     <firstname xsi:type="xsd:string">John</firstname>
     <lastname xsi:type="xsd:string">Hunsley</lastname>
     <email xsi:type="xsd:string">jphunsley@gmail.com</email>
     <icq xsi:type="xsd:string"/>
     <emailstop xsi:type="xsd:int">0</emailstop>
     <skype xsi:type="xsd:string"/>
     <yahoo xsi:type="xsd:string"/>
     <aim xsi:type="xsd:string"/>
     <msn xsi:type="xsd:string"/>
     <phone1 xsi:type="xsd:string"/>
     <phone2 xsi:type="xsd:string"/>
     <institution xsi:type="xsd:string"/>
     <department xsi:type="xsd:string"/>
     <address xsi:type="xsd:string"/>
     <city xsi:type="xsd:string">Shrewsbury</city>
     <country xsi:type="xsd:string">GB</country>
     <lang xsi:type="xsd:string">en_utf8</lang>
     <timezone xsi:type="xsd:int">99</timezone>
     <mnethostid xsi:type="xsd:int">1</mnethostid>
     <lastip xsi:type="xsd:string"/>
     <theme xsi:type="xsd:string"/>
     <description xsi:type="xsd:string"/>
     <role xsi:type="xsd:int">3</role>
     <profile SOAP-ENC:arrayType="ns1:profileitemRecord[0]" xsi:type="ns1:profileitemRecords"/>
     </item>
     * </p>
     * @param courseId
     * @return
     * @throws InvalidSessionException
     */
    public List<User> getTeachersByCourse(final int courseId) throws InvalidSessionException {
        if(sessionDAO == null) throw new InvalidSessionException();

        return getUsersByRoleAndCourse(courseId, User.TEACHER_ROLE, GET_TEACHERS_METHOD_NAME);
    }

    /**
     * <p>
     *
     * <soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsdl="http://localhost/moodle/wspp/wsdl">
     <soapenv:Header/>
     <soapenv:Body>
     <wsdl:get_messages soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
     <client xsi:type="xsd:int">89</client>
     <sesskey xsi:type="xsd:string">1a78ff3a77a6bb75b9dfdb0b915a5ca8</sesskey>
     <userid xsi:type="xsd:string">2</userid>
     <useridfield xsi:type="xsd:string">id</useridfield>
     </wsdl:get_messages>
     </soapenv:Body>
     </soapenv:Envelope>




     <ns1:get_messagesResponse>
     <return xsi:type="ns1:getMessagesReturn">
     <messages SOAP-ENC:arrayType="ns1:messageRecord[1]" xsi:type="ns1:messageRecords">
     <item xsi:type="ns1:messageRecord">
     <error xsi:type="xsd:string"/>
     <id xsi:type="xsd:int">13</id>
     <useridfrom xsi:type="xsd:int">3</useridfrom>
     <useridto xsi:type="xsd:int">2</useridto>
     <subject xsi:type="xsd:string"/>
     <fullmessage xsi:type="xsd:string">hi there, did this appear in soap ui?</fullmessage>
     <fullmessageformat xsi:type="xsd:int">0</fullmessageformat>
     <fullmessagehtml xsi:type="xsd:string"/>
     <smallmessage xsi:type="xsd:string">hi there, did this appear in soap ui?</smallmessage>
     <notification xsi:type="xsd:int">0</notification>
     <contexturl xsi:type="xsd:string"/>
     <contexturlname xsi:type="xsd:string"/>
     <timecreated xsi:type="xsd:int">1294146579</timecreated>
     <firstname xsi:type="xsd:string">Fred</firstname>
     <lastname xsi:type="xsd:string">Fred</lastname>
     <email xsi:type="xsd:string">fredbloggs@mymoodle.com</email>
     <picture xsi:type="xsd:string">0</picture>
     <imagealt xsi:type="xsd:string"/>
     </item>
     </messages>
     </return>
     </ns1:get_messagesResponse>

     java.net.SocketException: The operation timed out
W/System.err( 3176):    at org.apache.harmony.luni.platform.OSNetworkSystem.connectStreamWithTimeoutSocketImpl(Native Method)
W/System.err( 3176):    at org.apache.harmony.luni.platform.OSNetworkSystem.connect(OSNetworkSystem.java:115)
W/System.err( 3176):    at org.apache.harmony.luni.net.PlainSocketImpl.connect(PlainSocketImpl.java:244)
W/System.err( 3176):    at org.apache.harmony.luni.net.PlainSocketImpl.connect(PlainSocketImpl.java:533)
W/System.err( 3176):    at java.net.Socket.connect(Socket.java:1055)
W/System.err( 3176):    at org.apache.harmony.luni.internal.net.www.protocol.http.HttpConnection.<init>(HttpConnection.java:62)
W/System.err( 3176):    at org.apache.harmony.luni.internal.net.www.protocol.http.HttpConnectionPool.get(HttpConnectionPool.java:88)
W/System.err( 3176):    at org.apache.harmony.luni.internal.net.www.protocol.http.HttpURLConnectionImpl.getHTTPConnection(HttpURLConnectionImpl.java:927)
W/System.err( 3176):    at org.apache.harmony.luni.internal.net.www.protocol.http.HttpURLConnectionImpl.connect(HttpURLConnectionImpl.java:909)
W/System.err( 3176):    at org.ksoap2.transport.ServiceConnectionSE.connect(ServiceConnectionSE.java:46)
W/System.err( 3176):    at org.ksoap2.transport.HttpTransportSE.call(HttpTransportSE.java:69)
W/System.err( 3176):    at com.mobilevle.oktech.VLEHandlerOKTechImpl.getNewMessages(VLEHandlerOKTechImpl.java:576)
W/System.err( 3176):    at com.mobilevle.messenger.MessageSyncService.updateMessages(MessageSyncService.java:113)
W/System.err( 3176):    at com.mobilevle.messenger.MessageSyncService.access$000(MessageSyncService.java:28)
W/System.err( 3176):    at com.mobilevle.messenger.MessageSyncService$1$1.run(MessageSyncService.java:68)
     *
     * </p>
     * @return
     * @throws InvalidSessionException
     */
    public List<Message> getNewMessages() throws InvalidSessionException, IOException {
        if(sessionDAO == null) throw new InvalidSessionException();

        try {
            Session session = sessionDAO.loadSession();

            if(session == null) throw new InvalidSessionException();

            SoapObject request = new SoapObject(namespace, GET_MESSAGES_METHOD_NAME);
            request.addProperty("client", session.getClientId());
            request.addProperty("sesskey", session.getToken());
            request.addProperty("userid", session.getUserId());
            request.addProperty("useridfield", "id");
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
            androidHttpTransport.call(GET_MESSAGES_METHOD_NAME, envelope);
            SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
            SoapObject messagesResult = (SoapObject)resultsRequestSOAP.getProperty(0);
            SoapObject messagesRecords = (SoapObject)messagesResult.getProperty(0);
            return getMessages(messagesRecords);

        }  catch (XmlPullParserException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);
        }
    }


    /**
     * <p>
     *
     * <soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsdl="http://localhost/moodle/wspp/wsdl">
     <soapenv:Header/>
     <soapenv:Body>
     <wsdl:get_messages_history soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
     <client xsi:type="xsd:int">95</client>
     <sesskey xsi:type="xsd:string">922bc093cbf0986fd60fa464ad99a48f</sesskey>
     <useridto xsi:type="xsd:string">3</useridto>
     <useridtofield xsi:type="xsd:string">id</useridtofield>
     <useridfrom xsi:type="xsd:string">2</useridfrom>
     <useridfromfield xsi:type="xsd:string">id</useridfromfield>
     </wsdl:get_messages_history>
     </soapenv:Body>
     </soapenv:Envelope>


     <ns1:get_messages_historyResponse>
     <return xsi:type="ns1:getMessagesReturn">
     <messages SOAP-ENC:arrayType="ns1:messageRecord[8]" xsi:type="ns1:messageRecords">
     <item xsi:type="ns1:messageRecord">
     <error xsi:type="xsd:string"/>
     <id xsi:type="xsd:int">1</id>
     <useridfrom xsi:type="xsd:int">2</useridfrom>
     <useridto xsi:type="xsd:int">3</useridto>
     <subject xsi:type="xsd:string"/>
     <fullmessage xsi:type="xsd:string">hello how are you getting on with the quiz?</fullmessage>
     <fullmessageformat xsi:type="xsd:int">0</fullmessageformat>
     <fullmessagehtml xsi:type="xsd:string"/>
     <smallmessage xsi:type="xsd:string">hello how are you getting on with the quiz?</smallmessage>
     <notification xsi:type="xsd:int">0</notification>
     <contexturl xsi:type="xsd:string"/>
     <contexturlname xsi:type="xsd:string"/>
     <timecreated xsi:type="xsd:int">1288915775</timecreated>
     <firstname xsi:type="xsd:string">John</firstname>
     <lastname xsi:type="xsd:string">John</lastname>
     <email xsi:type="xsd:string">jphunsley@gmail.com</email>
     <picture xsi:type="xsd:string">0</picture>
     <imagealt xsi:type="xsd:string"/>
     </item>
     *
     * </p>
     * @param userId
     * @return
     * @throws InvalidSessionException
     */
    public List<Message> getConversation(final String userId) throws InvalidSessionException {
        if(sessionDAO == null) throw new InvalidSessionException();

        Session session = sessionDAO.loadSession();
        return getConversation(session.getUserId(), userId);
    }

    /**
     *
     * @param toUserId
     * @param fromUserId
     * @return
     * @throws InvalidSessionException
     */
    private List<Message> getConversation(final String toUserId, final String fromUserId) throws InvalidSessionException {
        if(sessionDAO == null) throw new InvalidSessionException();

        try {
            Session session = sessionDAO.loadSession();
            SoapObject request = new SoapObject(namespace, GET_MESSAGES_HISTORY_METHOD_NAME);
            request.addProperty("client", session.getClientId());
            request.addProperty("sesskey", session.getToken());
            request.addProperty("useridto", toUserId);
            request.addProperty("useridtofield", "id");
            request.addProperty("useridfrom", fromUserId);
            request.addProperty("useridfromfield", "id");
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
            androidHttpTransport.call(GET_MESSAGES_HISTORY_METHOD_NAME, envelope);
            SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
            SoapObject messagesResult = (SoapObject)resultsRequestSOAP.getProperty(0);
            SoapObject messagesRecords = (SoapObject)messagesResult.getProperty(0);
            return getMessages(messagesRecords);

        } catch (IOException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);

        } catch (XmlPullParserException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);
        }
    }

    /**
     *
     * @param messagesRecords
     * @return
     */
    private List<Message> getMessages(SoapObject messagesRecords) {
        List<Message> messages = new ArrayList<Message>();
        int messageCount = messagesRecords.getPropertyCount();

        for(int i = 0; i < messageCount; i++) {
            SoapObject messageRecord = (SoapObject)messagesRecords.getProperty(i);

            if(messageRecord.getProperty("error").toString().equalsIgnoreCase(NO_MESSAGES)) continue;

            User user = new User();
            user.setId(messageRecord.getProperty("useridfrom").toString());
            user.setFirstName(messageRecord.getProperty("firstname").toString());
            user.setLastName(messageRecord.getProperty("lastname").toString());
            user.setEmail(messageRecord.getProperty("email").toString());

            Message message = new Message();
            message.setFromUser(user);
            message.setContent(messageRecord.getProperty("fullmessage").toString());
            message.setSubject(messageRecord.getProperty("subject").toString());
            message.setId(Integer.parseInt(messageRecord.getProperty("id").toString()));
            message.setSendDate(new Date(Long.parseLong(messageRecord.getProperty("timecreated").toString())*1000));
            messages.add(message);
        }

        return messages;
    }

    /**
     * <error xsi:type="xsd:string"/>
                  <id xsi:type="xsd:int">2</id>
                  <auth xsi:type="xsd:string">manual</auth>
                  <confirmed xsi:type="xsd:int">1</confirmed>
                  <policyagreed xsi:type="xsd:int">0</policyagreed>
                  <deleted xsi:type="xsd:int">0</deleted>
                  <username xsi:type="xsd:string">admin</username>
                  <idnumber xsi:type="xsd:string"/>
                  <firstname xsi:type="xsd:string">John</firstname>
                  <lastname xsi:type="xsd:string">Hunsley</lastname>
                  <email xsi:type="xsd:string">jphunsley@gmail.com</email>
                  <icq xsi:type="xsd:string"/>
                  <emailstop xsi:type="xsd:int">0</emailstop>
                  <skype xsi:type="xsd:string"/>
                  <yahoo xsi:type="xsd:string"/>
                  <aim xsi:type="xsd:string"/>
                  <msn xsi:type="xsd:string"/>
                  <phone1 xsi:type="xsd:string"/>
                  <phone2 xsi:type="xsd:string"/>
                  <institution xsi:type="xsd:string"/>
                  <department xsi:type="xsd:string"/>
                  <address xsi:type="xsd:string"/>
                  <city xsi:type="xsd:string">Shrewsbury</city>
                  <country xsi:type="xsd:string">GB</country>
                  <lang xsi:type="xsd:string">en_utf8</lang>
                  <timezone xsi:type="xsd:int">99</timezone>
                  <mnethostid xsi:type="xsd:int">1</mnethostid>
                  <lastip xsi:type="xsd:string"/>
                  <theme xsi:type="xsd:string"/>
                  <description xsi:type="xsd:string"/>
                  <role xsi:type="xsd:int">0</role>
     * @param usersRecords
     * @return
     */
    private List<User> getUsers(SoapObject usersRecords) {
        List<User> users = new ArrayList<User>();
        int messageCount = usersRecords.getPropertyCount();

        for(int i = 0; i < messageCount; i++) {
            SoapObject userRecord = (SoapObject)usersRecords.getProperty(i);

            if(userRecord.getProperty("error").toString().equals(NO_MATCH)) continue;

            User user = new User();
            user.setId(userRecord.getProperty("id").toString());
            user.setFirstName(userRecord.getProperty("firstname").toString());
            user.setLastName(userRecord.getProperty("lastname").toString());
            user.setEmail(userRecord.getProperty("email").toString());
            user.setRole(Integer.parseInt(userRecord.getProperty("role").toString()));
            user.setUsername(userRecord.getProperty("username").toString());
            users.add(user);
        }
        
        Log.i("VLEHandlerOKTechImpl", "Found "+users.size()+" users");
        return users;
    }
    

    /**
     * <p>
     *
     * <soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsdl="http://localhost/moodle/wspp/wsdl">
     <soapenv:Header/>
     <soapenv:Body>
     <wsdl:message_send soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
     <client xsi:type="xsd:int">89</client>
     <sesskey xsi:type="xsd:string">1a78ff3a77a6bb75b9dfdb0b915a5ca8</sesskey>
     <userid xsi:type="xsd:string">3</userid>
     <useridfield xsi:type="xsd:string">id</useridfield>
     <message xsi:type="xsd:string">test from SOAP UI</message>
     </wsdl:message_send>
     </soapenv:Body>
     </soapenv:Envelope>


     <SOAP-ENV:Envelope SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns1="http://localhost/moodle/wspp/wsdl" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:SOAP-ENC="http://schemas.xmlsoap.org/soap/encoding/">
     <SOAP-ENV:Body>
     <ns1:message_sendResponse>
     <return xsi:type="ns1:affectRecord">
     <error xsi:type="xsd:string"/>
     <status xsi:type="xsd:boolean">true</status>
     </return>
     </ns1:message_sendResponse>
     </SOAP-ENV:Body>
     </SOAP-ENV:Envelope>
     *
     * </p>
     * @param message
     * @return
     * @throws InvalidSessionException
     */
    public boolean sendMessage(Message message) throws InvalidSessionException {
        if(sessionDAO == null) throw new InvalidSessionException();

        try {
            Session session = sessionDAO.loadSession();
            SoapObject request = new SoapObject(namespace, SEND_MESSAGE_METHOD_NAME);
            request.addProperty("client", session.getClientId());
            request.addProperty("sesskey", session.getToken());
            request.addProperty("userid", message.getToUser().getId());
            request.addProperty("useridfield", "id");
            request.addProperty("message", message.getContent());
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
            androidHttpTransport.call(SEND_MESSAGE_METHOD_NAME, envelope);
            SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
            SoapObject messageResult = (SoapObject)resultsRequestSOAP.getProperty(0);
            return Boolean.parseBoolean(messageResult.getProperty("status").toString());

        } catch (IOException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);

        } catch (XmlPullParserException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);
        }
    }

    /**
     * <soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsdl="http://localhost/moodle/wspp/wsdl" xmlns:soapenc="http://schemas.xmlsoap.org/soap/encoding/">
     <soapenv:Header/>
     <soapenv:Body>
     <wsdl:get_instances_bytype soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
     <client xsi:type="xsd:integer">40</client>
     <sesskey xsi:type="xsd:string">e3a8c2058c7805d34da41a3c2c683b55</sesskey>
     <courseids xsi:type="wsdl:getCoursesInput" soapenc:arrayType="xsd:string[1]">
     <item xsi:type="xsd:string">3</item>
     </courseids>
     <idfield xsi:type="xsd:string">id</idfield>
     <type xsi:type="xsd:string">lesson</type>
     </wsdl:get_instances_bytype>
     </soapenv:Body>
     </soapenv:Envelope>


     get_instances_byid{client=50; sesskey=809e60711560903cb2c7e6a02aa1bb5f; courseids=getCoursesInput{item=2; }; idfield=id; type=lesson; }


     <SOAP-ENV:Envelope SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns1="http://localhost/moodle/wspp/wsdl" xmlns:SOAP-ENC="http://schemas.xmlsoap.org/soap/encoding/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     <SOAP-ENV:Body>
     <ns1:get_instances_bytypeResponse>
     <return xsi:type="ns1:getResourcesReturn">
     <resources SOAP-ENC:arrayType="ns1:resourceRecord[1]" xsi:type="ns1:resourceRecords">
     <item xsi:type="ns1:resourceRecord">
     <error xsi:type="xsd:string"/>
     <id xsi:type="xsd:integer">1</id>
     <name xsi:type="xsd:string">Collections Lesson</name>
     <course xsi:type="xsd:integer">2</course>
     <type xsi:type="xsd:string"/>
     <reference xsi:type="xsd:string"/>
     <summary xsi:type="xsd:string"/>
     <alltext xsi:type="xsd:string"/>
     <popup xsi:type="xsd:string"/>
     <options xsi:type="xsd:string"/>
     <timemodified xsi:type="xsd:integer">1290627747</timemodified>
     <section xsi:type="xsd:integer">0</section>
     <visible xsi:type="xsd:integer">1</visible>
     <groupmode xsi:type="xsd:integer">0</groupmode>
     <coursemodule xsi:type="xsd:integer">4</coursemodule>
     <url xsi:type="xsd:string">http://localhost/moodle/mod/lesson/view.php?id=4</url>
     <timemodified_ut xsi:type="xsd:string">Wednesday,  24 November 2010, 07:42 PM</timemodified_ut>
     </item>
     </resources>
     </return>
     </ns1:get_instances_bytypeResponse>
     </SOAP-ENV:Body>
     </SOAP-ENV:Envelope>
     */

    /**
     * <soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsdl="http://localhost/moodle/wspp/wsdl">
   <soapenv:Header/>
   <soapenv:Body>
      <wsdl:get_user soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
         <client xsi:type="xsd:int">181</client>
         <sesskey xsi:type="xsd:string">ab6beef40fce72d1be66794edb9244e9</sesskey>
         <userid xsi:type="xsd:string">hunsley</userid>
         <idfield xsi:type="xsd:string">lastname</idfield>
      </wsdl:get_user>
   </soapenv:Body>
</soapenv:Envelope>


     <ns1:get_userResponse>
         <return xsi:type="ns1:getUsersReturn">
            <users SOAP-ENC:arrayType="ns1:userRecord[1]" xsi:type="ns1:userRecords">
               <item xsi:type="ns1:userRecord">
                  <error xsi:type="xsd:string"/>
                  <id xsi:type="xsd:int">2</id>
                  <auth xsi:type="xsd:string">manual</auth>
                  <confirmed xsi:type="xsd:int">1</confirmed>
                  <policyagreed xsi:type="xsd:int">0</policyagreed>
                  <deleted xsi:type="xsd:int">0</deleted>
                  <username xsi:type="xsd:string">admin</username>
                  <idnumber xsi:type="xsd:string"/>
                  <firstname xsi:type="xsd:string">John</firstname>
                  <lastname xsi:type="xsd:string">Hunsley</lastname>
                  <email xsi:type="xsd:string">jphunsley@gmail.com</email>
                  <icq xsi:type="xsd:string"/>
                  <emailstop xsi:type="xsd:int">0</emailstop>
                  <skype xsi:type="xsd:string"/>
                  <yahoo xsi:type="xsd:string"/>
                  <aim xsi:type="xsd:string"/>
                  <msn xsi:type="xsd:string"/>
                  <phone1 xsi:type="xsd:string"/>
                  <phone2 xsi:type="xsd:string"/>
                  <institution xsi:type="xsd:string"/>
                  <department xsi:type="xsd:string"/>
                  <address xsi:type="xsd:string"/>
                  <city xsi:type="xsd:string">Shrewsbury</city>
                  <country xsi:type="xsd:string">GB</country>
                  <lang xsi:type="xsd:string">en_utf8</lang>
                  <timezone xsi:type="xsd:int">99</timezone>
                  <mnethostid xsi:type="xsd:int">1</mnethostid>
                  <lastip xsi:type="xsd:string"/>
                  <theme xsi:type="xsd:string"/>
                  <description xsi:type="xsd:string"/>
                  <role xsi:type="xsd:int">0</role>
                  <profile SOAP-ENC:arrayType="ns1:profileitemRecord[0]" xsi:type="ns1:profileitemRecords"/>
               </item>
            </users>
         </return>
      </ns1:get_userResponse>
     * @param field
     * @param value
     * @return
     * @throws InvalidSessionException
     */
     public List<User> searchUsers(final String field, final String value) throws InvalidSessionException {
        if(sessionDAO == null) throw new InvalidSessionException();
        Log.i("VLEHandlerOKTechImpl", "Searching user field "+field+" for value "+value);
        
        try {
            Session session = sessionDAO.loadSession();
            SoapObject request = new SoapObject(namespace, GET_USER_METHOD_NAME);
            request.addProperty("client", session.getClientId());
            request.addProperty("sesskey", session.getToken());
            request.addProperty("userid", value);
            request.addProperty("idfield", field);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
            androidHttpTransport.call(GET_USER_METHOD_NAME, envelope);
            SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
            SoapObject usersResult = (SoapObject)resultsRequestSOAP.getProperty(0);
            SoapObject usersRecords = (SoapObject)usersResult.getProperty(0);
            return getUsers(usersRecords);

        } catch (IOException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);

        } catch (XmlPullParserException e) {
            Log.e("VLEHandlerOKTechImpl", e.getMessage());
            throw new InvalidSessionException(e);
        }
    }
}
