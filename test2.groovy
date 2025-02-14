import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.amdocs.bil.notification.data.model.NotificationMessage
import com.amdocs.bil.notification.data.model.Sender
import com.amdocs.bil.notification.data.model.Sms
import com.amdocs.bil.notification.util.AuthorizationUtil
import com.amdocs.bil.notification.util.RouteHelperNotif
import org.apache.camel.ProducerTemplate
import com.amdocs.bl.common.util.ValidationException
import com.amdocs.bl.common.util.CommonBlfProductUtils

Logger log = LoggerFactory.getLogger('com.amdocs.bl.notification.groovy')

log.info('(orderingRemoveAddon): Begin')

// authenticates session
new AuthorizationUtil(exchange).authenticate()

log.info('(orderingRemoveAddon): Authenticated')

// ?????????
def notif = exchange.getProperty('notificationMessage', NotificationMessage)

// Producer template includes the ?????
def producerTemplate = exchange.getProperty('producerTemplate', ProducerTemplate)

log.info("(orderingRemoveAddon): Setting up BLFUTILS...")
//BLF Utils
CommonBlfProductUtils blfUtils = new CommonBlfProductUtils()

// BIL API invokation helper
def routeHelper = new RouteHelperNotif(exchange, producerTemplate)
log.info('(orderingRemoveAddon): Route Helped Initialized')

// Notification data comes directly from RHT
def notificationData = notif.getNotificationRequest().getNotificationData()
log.info('(orderingRemoveAddon): RHT Details Fetched')

// extract data from RHT
def tenantId = notificationData.get('tenantId')
log.info("(orderingRemoveAddon): tenantId: " + tenantId)

def accountNo = notificationData.get('accountInternalId')
log.info("(orderingRemoveAddon): accountNo: " + accountNo)

def serviceInternalId = notificationData.get('serviceInternalId')
log.info("(orderingRemoveAddon): serviceInternalId: " + serviceInternalId)

def serviceInternalIdResets = notificationData.get('serviceInternalIdResets')
log.info("(orderingRemoveAddon): serviceInternalIdResets: " + serviceInternalIdResets)

def accountSegmentId = notif.getAccountSegmentId().toString()
log.info("(orderingRemoveAddon): accountSegmentId: " + accountSegmentId)

def offerId = notificationData.get('OfferId')
log.info("(orderingRemoveAddon): OfferId: " + offerId)

def offerName = notificationData.get('offerName')
log.info("(orderingRemoveAddon): offerName: " + offerName)

def LOCALE_HEADER_NAME = 'Accept-Language'

def ut
def path = new File(".").absolutePath
log.info("updateAccountOwnerDetails.GROOVY: : STARTED ----- path="+path)
GroovyShell shell = new GroovyShell()
if( path.contains("\\bil-notification") ){    //Unit tests on Windows VM
        ut = shell.parse(new File("src\\test\\resources\\groovy\\utils.groovy"))
}
else if( path.contains("/bil-notification") ){    //integration tests in Jenkins Linux container
        ut = shell.parse(new File("target/test-classes/groovy/utils.groovy"))
}
else {
    ut = shell.parse(new File("etc/groovy/utils.groovy"))  //production running off <JBOSS_HOME>
}

// must be set so it can be parsed later
def result = 1

// List of phone numbers where the notification will be sent to
List<String> phoneNumbers = []

def offerName_en = offerName
def offerName_kz = offerName
def offerName_ru = offerName
def headLineFeature_kz = ""
def headLineFeature_ru = ""
def chargeType = ""



try {
    // Fetch customeraccount/individuals to retrieve preffered language & contact number
    def call1 = ut.getAccountSegment(exchange);
    ut.getMembershipRoleType(exchange);
// Service Inventory API -> to retrieve the preferredLanguageCode
    def call2 = ut.getPreferedLanguageCode(exchange);
    preferredLanguageCode = notificationData.get('preferredLanguageCode')
    log.info('(orderingRemoveAddon): preferredLanguageCode ->' + preferredLanguageCode)
    log.info('(orderingRemoveAddon): preferredLanguageCode ->' + call2)

    log.info('(orderingRemoveAddon): Fetching customeraccounts individuals...')

    //roleSpecId is set to -1 because we always need primary email
    //def call3 = ut.getPrimaryNumber(exchange,accountNo);
    //phoneNumbers.add(call3)

    log.info('(orderingRemoveAddon): PHONENUMS   --->>>' + phoneNumbers)

} catch (Exception ex) {
    log.error('(orderingRemoveAddon): Error during customeraccounts invokation and data extraction', ex)
}

// Fetch customeraccount/individuals to retrieve msisdn
try {


    // Fetch customeraccount/individuals to retrieve msisdn
    // API call in order to retrieve the MSISDN NUM
    def call4 = ut.getMSISDNNumber(exchange,accountNo);

    if (!phoneNumbers.contains(call4)) {
        phoneNumbers.add(call4)
    } else {
        log.info('(orderingRemoveAddon): MSISDN already has serviceExternalId')
    }

    log.info('(orderingRemoveAddon): PHONENUMS   --->>>' + phoneNumbers)

}
catch (Exception ex) {
    log.info(ex)
}

log.info('(orderingRemoveAddon): Configuring notification based on Language: ' + preferredLanguageCode)

        def shortDisplayUndefinedRu = 'ru-RU'
        def shortDisplayUndefinedKz = 'kk-KZ'
        def call5 = ut.getEnumerationLanguage(exchange,preferredLanguageCode);
        def shortDisplayValue = notificationData.get('shortDisplay')
        def languageNameValue = notificationData.get('languageName')
        log.info('(orderingRemoveAddon): shortDisplayValue ->' + shortDisplayValue)
        log.info('(orderingRemoveAddon): languageNameValue ->' + languageNameValue)


        if (languageNameValue.equals("Undefined")){

            log.info('(orderingAddAddOn): switch: ' + languageNameValue + ' ' + preferredLanguageCode)
            (offerName, headLineFeature, chargeType) = getLanguageBasedOfferName(shortDisplayUndefinedRu, offerId, log, routeHelper, producerTemplate)
            notificationData.put("offerName", offerName)
            notificationData.put("headLineFeature", headLineFeature)
            notificationData.put("chargeType", chargeType)


            (offerName_2, headLineFeature_2, chargeType) = getLanguageBasedOfferName(shortDisplayUndefinedKz, offerId, log, routeHelper, producerTemplate)
            notificationData.put("offerName_2", offerName_2)
            notificationData.put("headLineFeature_2", headLineFeature_2)
            notificationData.put("chargeType", chargeType)
            notif.getNotificationRequest().setLanguageCode(shortDisplayValue)

        }else{ //Russian & Kazakh
            log.info('(orderingAddAddOn): switch: ' + languageNameValue + ' ' + preferredLanguageCode)
            (offerName, headLineFeature, chargeType) = getLanguageBasedOfferName(shortDisplayValue, offerId, log, routeHelper, producerTemplate)
            notificationData.put("offerName", offerName)
            notificationData.put("headLineFeature", headLineFeature)
            notificationData.put("chargeType", chargeType)
            notif.getNotificationRequest().setLanguageCode(shortDisplayValue)
        }

if (chargeType.equals('NRC')) {
    exchange.setProperty("skipDispatcherMsg", (true));
}

log.info('(orderingRemoveAddon): Account Segment Skip Check...')

notif.setChannel('sms')
notif.setQueue('smsQueue')

Sms sms = new Sms()
sms.setPhoneNumbers(phoneNumbers)
log.info('(orderingRemoveAddon): Phone Numbers' + phoneNumbers)
log.info('(orderingRemoveAddon): sms' + sms.getPhoneNumbers())

notif.setSmsInfo(sms)
notif.setAccountNo(accountNo)
log.info('(orderingRemoveAddon): notif' + notif.getSmsInfo().getPhoneNumbers())

List<NotificationMessage> arrNotif = []

arrNotif.addAll(notif)
log.info('(orderingRemoveAddon): notif' + notif)

exchange.setProperty(com.amdocs.bil.notification.util.NotificationConstants.MULTI_MESSAGES, arrNotif)

log.info('(orderingRemoveAddon): Before setting result and exiting.')
Map<String, Object> variables = new HashMap<>()
variables.put('result', result)
exchange.setProperty('output', variables)
log.info('(orderingRemoveAddon): Log.............. ' + variables)
log.info('(orderingRemoveAddon): FINISH.............. Remove Add-On Groovy')


def getLanguageBasedOfferName(languageCode, offerId, log, routeHelper, producerTemplate) {

    log.info("(orderingRemoveAddon): Inside getLanguageBasedOfferName..")
    log.info("(orderingRemoveAddon): languageCode " + languageCode)
    log.info("(orderingRemoveAddon): offerId " + offerId)

    log.info("(orderingRemoveAddon): Setting up exchange headers...")

    exchange.getIn().setHeader("Accept-Language", languageCode)

    log.info("Fetching " + "Accept-Language" + " header: " + exchange.getIn().getHeader("Accept-Language"))

    log.info("(orderingRemoveAddon): Setting new routeHelper with new exchange header...")

    routeHelper = new RouteHelperNotif(exchange, producerTemplate)

    log.info("(orderingRemoveAddon): Fetching " + languageCode + " Offer Name...")

    def return_offer_name = ''
    def remove = ""

    try {
        response = routeHelper.invokeMicroservice(
                'bil.productcatalog.ms',
                'GET',
                '/productofferings/' + offerId,
                "fields=*",
                Map.class
        )
                type = ""
        log.info("(orderingRemoveAddon): After request get offering" + response)


        if (response.containsKey('productOfferingCharge')) {
            def productOfferingCharge = response.get('productOfferingCharge')
            type = productOfferingCharge[0].get('type');
        }
        offer_name = response.get('name')
                //offer_LongDiscription = response.get('longDescription')
        if (response.containsKey('headlineFeature')) {
            offer_LongDiscription = response.get('headlineFeature')
        } else {
            offer_LongDiscription = ''
        }
                //if (response.containsKey('longDescription')) {
          //  offer_LongDiscription = response.get('longDescription')
        //} else {
          //  offer_LongDiscription = ''
        //}


    }
    catch (Exception e) {
        log.info("(orderingRemoveAddon): Error while fetching translated offer name")
    }
    return [offer_name, offer_LongDiscription, type]

}