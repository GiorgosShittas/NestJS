import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amdocs.bil.notification.data.model.NotificationMessage;
import com.amdocs.bil.notification.data.model.Email;
import com.amdocs.bil.notification.data.model.Sms;
import com.amdocs.bil.notification.util.AuthorizationUtil;
import com.amdocs.bil.notification.util.RouteHelperNotif;
import com.amdocs.bil.notification.data.model.EmailAttachment;
import org.apache.camel.ProducerTemplate;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jackson.map.util.ISO8601DateFormat;
import com.amdocs.bl.common.util.ValidationException;

import java.io.File;
import java.io.FileInputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

Logger log = LoggerFactory.getLogger("com.amdocs.bl.notification.groovy");

log.info("START..............Case Common Groovy");

def result = 1;

new AuthorizationUtil(exchange).authenticate();

def notif = exchange.getProperty("notificationMessage", NotificationMessage.class);
def producerTemplate = exchange.getProperty("producerTemplate", ProducerTemplate.class);
def routeHelper = new RouteHelperNotif(exchange,producerTemplate);

def processInstData = notif.getNotificationRequest().getNotificationData().get("processInstanceData");
def accountData = notif.getNotificationRequest().getNotificationData().get("accountData");
String requestChannel;
//String requestChannel = (String)notif.getNotificationRequest().getNotificationData().get("channel");
String caseId = processInstData.get("processExternalId");
def processInstanceDocId = processInstData.get("processInstDocId");
String newCaseId = caseId.tokenize("-").get(1);
//ZoneId timeZone = notif.getNotificationRequest().getNotificationData().get("zoneId");

def caseType;
def caseSubType;
def caseQueue;
String purpose = "Case Management";
def preferredLanguageCode = null;

//Send only to SMS channel based on these types.
//Case Type                        Case Type Id          Sub Type                        Subtype Id      SMS     EMAIL
//1. Качество обслуживания         1000                  1.1. Претензия на сотр. СС      2000            Yes     No
//2. Инцидент                                      1003                                  2.1. Единичный инцидент                 2003            Yes     No
//2. Инцидент                                      1003                                  2.2. Массовый инцидент                  2006            Yes     No

def processTypeId = processInstData.get("processTypeId");
def processSubTypeId = processInstData.get("processSubTypeId");
def processTypeArray = ["1000", "1003", "1006", "1009", "1012", "1015", "1018", "1021", "1023"] as String[];
def processSubTypeArray = ["2000", "2003", "2006", "2009", "2012", "2015", "2018", "2021", "2023", "2025", "2027", "2029", "2031", "2033", "2035", "2037"] as String[];

log.info("processTypeId ........................." +processTypeId);
log.info("processSubTypeId........................." +processSubTypeId);
log.info("Notification processing started for caseid " +caseId+ " for VEON ");

log.debug("case-id: " +caseId+ " Notification Payload at this instance is -"+notif.getNotificationRequest().getNotificationData().toString());

log.info("case-id: " +caseId+ " Eliminating Case Prefix...");
log.info("case-id: " +newCaseId+ " After Eliminating Case Prefix...");
log.info("case-id: " +newCaseId+ " will be used for all customer notifications...");

def acctInternalId=accountData.get("accountInternalId");
String contactMobNo=null;
List<String> phoneNumbers = new ArrayList<String>();


def serviceStatuses = null;
String statusId = null;
Boolean skip = false;
String serviceExternalId;
String serviceDisplayValue;
def serviceKey;
def serviceInternalId = null;
def serviceInternalIdResets = null;


String customerName =null;
def  caseSLADuration=null;
def stateSLADuration=null;
String contactFname=null;
String contactLname=null;
String contactEmail=null;
String emailSubject=null;


//Evoke BIL API call to find the account contact information

 try {
        log.info("case-id: " +caseId+ " Calling BIL API  to get Customer Account Details ");

         def telPriority=0;
         def emailPriority=0;
         def setPriority=0;// Use 0 for taking maximum value as highest priority and 1 for vice versa

        //Check URL
        //roleSpecId=-1 as recipient should always be the primaryContact Email
        def response = routeHelper.invokeMicroservice("bil.contact.ms","GET","/customeraccounts/"+acctInternalId+"/individuals","hierarchy=false&embed=partyContactMedium&fields=individualRole&roleSpecId=-1", ArrayList.class);

        log.info("case-id: " +caseId+ "evoking BIL API  to get Customer Account Details completed");

        if (response.size() > 0 && response.get(0).containsKey("individualId")) {

                        log.info("case-id: " +caseId+ " Inside If clause for response size check in Customer Account Details ");

                        contactFname=response.get(0).get("firstName");
                        log.info("case-id: " +caseId+ " firstName "+contactFname);
                        contactLname=response.get(0).get("lastName");
                        log.info("case-id: " +caseId+ " lastName "+contactLname);
                        log.info("case-id: " +caseId+ " response size "+ response.size());
                        log.info("case-id: " +caseId+ " response to string "+ response.toString());

                        individualRole=response.get(0).get("individualRole");
                        log.info("case-id: " +caseId+ "  After individual role"+individualRole.toString());

                        if(individualRole.containsKey("emailContacts")){
                                def emailContacts=individualRole.get("emailContacts");
                                log.info("case-id: " +caseId+ " Size of emailContacts array "+emailContacts.size());
                                contactEmail=emailContacts.get(0).get("emailAddress");
                        } else{
                                log.info("WARNING emailContacts does not exist for:"+acctInternalId);
                        }


                        if(individualRole.containsKey("telephoneContacts")){
                                def telephoneContacts=individualRole.get("telephoneContacts");
                                log.info("case-id: " +caseId+ " Size of telephoneContacts array "+telephoneContacts.size());
                                contactMobNo=telephoneContacts.get(0).get("phoneNumber");
                                //phoneNumbers.add(contactMobNo);
                                log.info("contactNumber ******:"+contactMobNo);
                        } else {
                                log.info("WARNING phoneContacts does not exist for:"+acctInternalId);
                        }


                        customerName="$contactFname $contactLname";

                        log.info("case-id: " +caseId+ " customerName: "+customerName);
                        log.info("case-id: " +caseId+ " contactEmail: "+contactEmail);
                        //log.info("case-id: " +caseId+ " contactMobNo: "+contactMobNo);
                        log.info("case-id: " +caseId+ " All contact information required for customer notifications obtained successfully");

        } else
        {
                //throw new Exception("Contact Information not found for account "+
                throw new ValidationException("Contact Information not found for account "+acctInternalId,null,null,null);
                }
} catch (Exception e) {
        //log.error("Error retrieving state spec display value",e);
        log.error("case-id: " +caseId+ " Error Calling BIL API  to get Customer Account Details ",e);
}





try
{
def response = routeHelper.invokeMicroservice("bil.serviceinventory.ms","GET","/customeraccounts/"+acctInternalId+"/customerfacingservices","limit=100&embed=cfsStatus,inventory", ArrayList.class);
        //log.info("Services under account: "+acctInternalId+", "+response);
        if(null != response && response.size() > 0){

                skip = false;
                                log.info("serviceStatus ******:"+response);

                for(service in response){

                        serviceStatuses = service.get("serviceStatuses");
                                                serviceInternalId = service.get("serviceInternalId");
                                                serviceInternalIdResets = service.get("serviceInternalIdResets");

                        if(serviceStatuses != null){
                                for(serviceStatus in serviceStatuses){
                                        log.info("serviceStatus ******:"+serviceStatus);
                                        statusId = (String)serviceStatus.get("statusId");
                                        log.info("statusId ******:"+statusId);
                                        if(statusId != null && (statusId.equals("2") || statusId.equals("4"))){
                                                skip = true;
                                        }
                                }
                        }

                                                                                if(skip){
                                log.info("Skipping Service as it's inactive.");
                                                                serviceDisplayValue = (String)service.get("serviceExternalIdType").get("displayValue");
                                                                serviceKey = service.get("serviceExternalIdType").get("key");
                                                                if (serviceKey.equals("11") || serviceDisplayValue.toLowerCase().equals("msisdn voice")){
                                log.info("getServiceMSISDN(): Service MSISDN: "+(String)service.get("serviceExternalId"));
                                serviceExternalId = (String)service.get("serviceExternalId");
                                                                }
                                continue;

                        }

                        serviceDisplayValue = (String)service.get("serviceExternalIdType").get("displayValue");
                        serviceKey = service.get("serviceExternalIdType").get("key");
                if (serviceKey.equals("11") || serviceDisplayValue.toLowerCase().equals("msisdn voice")){
                                log.info("getServiceMSISDN(): Service MSISDN: "+(String)service.get("serviceExternalId"));

                                serviceExternalId = (String)service.get("serviceExternalId");

                                if(!phoneNumbers.contains(serviceExternalId)){
                    phoneNumbers.add(serviceExternalId);
                } else {
                    log.info("getServiceMSISDN() phoneNumbers already has serviceExternalId");
                }
                        }

                }

        } else {
                log.info("Service MSISDN WARNING: response is empty or does not exist:"+response);
        }

} catch (Exception e) {
        //log.error("Error retrieving state spec display value",e);
        log.error("case-id: " +caseId+ " Error Calling BIL API  to get service MSISDN ",e);
}

log.info("WARNING phoneNumbers size :"+phoneNumbers.size());

if(phoneNumbers.size() == 0 && contactMobNo != serviceExternalId){
phoneNumbers.add(contactMobNo);
}

try
{
log.info("get preferredLanguageCode");
Map response2 = routeHelper.invokeMicroservice(
            'bil.serviceinventory.ms',
            'GET',
            '/customeraccounts/' + acctInternalId +
            '/customerfacingservices/' + serviceInternalId +'%2C' + serviceInternalIdResets,
            'embed=extData',
            Map.class,
            );

                        log.info('Case Common Response from EXT DATA API: ' + response2);

                        if(response2.size() > 0 && response2.containsKey('cfsExtDatas')){
            for (ext in response2.get('cfsExtDatas')) {
                if (ext.get('paramName').equals('ServiceLanguage')) {
                    preferredLanguageCode = (String) ext.get('paramValue');
                    log.info('Case Common preferredLanguageCode found : ' + preferredLanguageCode)

                }
            }
                        }
                                else {
                                preferredLanguageCode = 1;
                                log.info('Case Common Could not find Preffered Language code')
                                }

} catch (Exception e) {
        //log.error("Error retrieving state spec display value",e);
        log.error("case-id: " +caseId+ " Error Calling BIL API  to get langauage ",e);
}

if(preferredLanguageCode == null)
{
preferredLanguageCode = 1;
}


requestChannel="both";

if(processTypeArray.contains((String)processTypeId) && processSubTypeArray.contains((String)processSubTypeId)){
        log.info("PROCCESS TYPE ID:"+processTypeId+" SUBTYPE ID:"+processSubTypeId+" SEND ONLY TO SMS.");
        requestChannel="sms";
                //requestChannel="email";
}

log.info( "case-id: " +caseId+ " Notification Channel is - " + requestChannel);



//contactEmail = accountData.get("custEmail");
//log.info( "AccountData Email is : " + contactEmail);

String formattedDate = "NaN";

String slaDueDate = (String) processInstData.get("slaDueDate");
// set date/time properties
//ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.parse(slaDueDate), timeZone); // convert to zdt object
//String parsedDate = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss z yyyy").format(zdt); // format output e.g "Thu Mar 31 11:30:00 PHT 2022"

String parsedDate = "Thu Mar 31 11:30:00 PHT 2022";

//notif.getNotificationRequest().setLanguageCode("en-US");

switch(preferredLanguageCode) {

    case "19": //russian
        notif.getNotificationRequest().setLanguageCode('ru-RU');
        break

    case "27": //kazakh
        notif.getNotificationRequest().setLanguageCode('kk-KZ');
        break

    case "1": //undefined
        notif.getNotificationRequest().setLanguageCode('undefined');
        break

    default:
        break
}


switch(requestChannel){
        case "sms":
            notif.setChannel("sms");
            notif.setQueue("smsQueue");

                    Sms sms = new Sms();
                        //List<String> phoneNumbers = new ArrayList<String>();
                        //phoneNumbers.add(contactMobNo);
                    sms.setPhoneNumbers(phoneNumbers);

                    notif.setSmsInfo(sms);
                                        notif.setAccountNo(acctInternalId);
            break;

        case "email":
                notif.setChannel("email");
            notif.setQueue("emailQueue");

            Email email = new Email();
            email.setEmailTo(contactEmail);
            email.setSubject("Case Created");

            notif.setEmailInfo(email);
                        notif.setAccountNo(acctInternalId);
            break;

                case "both":
                //SMS SETUP
            notifSMS.setQueue("smsQueue");
            notifSMS.setChannel("sms");

            Sms sms = new Sms();
            sms.setPhoneNumbers(phoneNumbers);

            notifSMS.setSmsInfo(sms);
                        notifSMS.setAccountNo(acctInternalId);

                //EMAIL SETUP
            notif.setQueue("emailQueue");
                    notif.setChannel("email");
            Email email = new Email();
            email.setEmailTo(contactEmail);
            email.setSubject("Case Created");

            notif.setEmailInfo(email);
                        notif.setAccountNo(acctInternalId);

                //Adding Both Messages to Notification Array
                List<NotificationMessage> arrNotif = new ArrayList<>();
                arrNotif.add(notif);
                arrNotif.add(notifSMS);
                exchange.setProperty(com.amdocs.bil.notification.util.NotificationConstants.MULTI_MESSAGES, arrNotif);
                break;


            default:
                rt = 0; //fail as unknown channel provided
                log.error("case-id: " +caseId+ " " +requestChannel + "Unknown Notification Channel");

}


notif.getNotificationRequest().getNotificationData().put("caseId", caseId);
notif.getNotificationRequest().getNotificationData().put("slaInfo", parsedDate);

Map<String, Object> variables = new HashMap<>();
variables.put("result",result);
exchange.setProperty("output",variables);
log.info("Log.............. "+variables);
log.info("FINISH.............. Case Common Groovy");