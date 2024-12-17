package uk.co.dalelane.demos.ibmmq;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.github.javafaker.Commerce;
import com.github.javafaker.Faker;

import uk.co.dalelane.demos.ibmmq.App;

public class Putter extends App {

    private static final Faker faker = new Faker(Locale.UK);
    private static final Random rng = new Random();
    private static DateTimeFormatter dtFormatter = null;

    public static void main(String[] args) {
        
        
        try {
            dtFormatter = DateTimeFormatter.ofPattern(getStringEnv("TIMESTAMP_FORMAT"));
            int messageInterval = getIntEnv("MSG_INTERVAL");

            int maxNumProducts = 1 + rng.nextInt(getIntEnv("MAX_PRODUCTS"));


            Connection connection = createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(getStringEnv("MQ_QUEUE"));
            MessageProducer producer = session.createProducer(destination);
            connection.start();

            while (true) {
                //producer.send(session.createTextMessage(generateMessage(MESSAGE_TEMPLATE)));
                producer.send(session.createTextMessage(generateXMLMessage(maxNumProducts)));
                if (messageInterval != 0) Thread.sleep(messageInterval);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } 
        
    }

    private static String generateXMLMessage(int maxProducts) throws ParserConfigurationException, TransformerException, JMSException{
        Faker testF = new Faker();

        DocumentBuilderFactory factory =DocumentBuilderFactory.newInstance();

         DocumentBuilder docBuilder = factory.newDocumentBuilder();
 
 
        Document doc = docBuilder.newDocument();
     
        Element rootElement = doc.createElement("ordermessage");
         doc.appendChild(rootElement);
         
         Element child = doc.createElement("id");
         child.appendChild(doc.createTextNode(UUID.randomUUID().toString()));   
         rootElement.appendChild(child);
         
         Element el_customer = doc.createElement("customer");
         el_customer.appendChild(doc.createElement("id")).appendChild(doc.createTextNode(UUID.randomUUID().toString()));
         el_customer.appendChild(doc.createElement("name")).appendChild(doc.createElement("firstname")).appendChild(doc.createTextNode(faker.name().firstName()));
         el_customer.appendChild(doc.createElement("name")).appendChild(doc.createElement("lastname")).appendChild(doc.createTextNode(faker.name().lastName()));
         Element el_phone = doc.createElement("phone");
         boolean useCellPhone = rng.nextBoolean();
         el_phone.setAttribute("type", useCellPhone ? "landline" : "mobile");
         el_phone.setAttribute("number", useCellPhone ? faker.phoneNumber().phoneNumber() : faker.phoneNumber().cellPhone());
         el_customer.appendChild(el_phone);
         rootElement.appendChild(el_customer);
         
         Element el_creditCard = doc.createElement("creditcard");
         el_creditCard.appendChild(doc.createElement("number")).appendChild(doc.createTextNode(faker.finance().creditCard()));
         el_creditCard.appendChild(doc.createElement("expiry")).appendChild(doc.createTextNode(getCreditCardExpiry()));
         rootElement.appendChild(el_creditCard);

        for (int i = 0; i < maxProducts; i++) {
            Element el_product = doc.createElement("products");
            Commerce commerce = faker.commerce();
            el_product.appendChild(doc.createElement("id")).appendChild(doc.createTextNode(UUID.randomUUID().toString()));
            el_product.appendChild(doc.createElement("description")).appendChild(doc.createTextNode(commerce.productName()));
            el_product.appendChild(doc.createElement("cost")).appendChild(doc.createTextNode(commerce.price()));
            el_product.appendChild(doc.createElement("quantity")).appendChild(doc.createTextNode(Integer.toString(1 + rng.nextInt(2))));

            rootElement.appendChild(el_product);             
        }

         Element el_ordertime = doc.createElement("ordertime");
         el_ordertime.appendChild(doc.createTextNode(dtFormatter.format(ZonedDateTime.now())));
         rootElement.appendChild(el_ordertime);


        StringWriter outputXML = new StringWriter();
        
         TransformerFactory transformerFactory = TransformerFactory.newInstance();
 Transformer transformer = transformerFactory.newTransformer();
 transformer.transform(new DOMSource(doc), new StreamResult(outputXML));
 
 return outputXML.toString();
    }
/*
    private static String generateMessage(String template) {
        Name name = faker.name();
        Commerce commerce = faker.commerce();
        PhoneNumber phonenumber = faker.phoneNumber();
        boolean useCellPhone = rng.nextBoolean();
        return template
            .replace("1_TEMPLATE_ORDER_ID", UUID.randomUUID().toString())
            .replace("2_TEMPLATE_CUSTOMER_ID", UUID.randomUUID().toString())
            .replace("3_TEMPLATE_CUSTOMER_FIRSTNAME", name.firstName())
            .replace("4_TEMPLATE_CUSTOMER_LASTNAME", name.lastName())
            .replace("5_TEMPLATE_PHONE_TYPE", useCellPhone ? "landline" : "mobile")
            .replace("6_TEMPLATE_PHONE_NUMBER", useCellPhone ? phonenumber.phoneNumber() : phonenumber.cellPhone())
            .replace("7_TEMPLATE_CREDITCARD_NUMBER", faker.finance().creditCard())
            .replace("8_TEMPLATE_CREDITCARD_EXPIRY", getCreditCardExpiry())
            .replace("9_TEMPLATE_PRODUCT_ID", UUID.randomUUID().toString())
            .replace("10_TEMPLATE_PRODUCT_DESCRIPTION", commerce.productName())
            .replace("11_TEMPLATE_PRODUCT_COST", commerce.price())
            .replace("12_TEMPLATE_PRODUCT_QUANTITY", Integer.toString(1 + rng.nextInt(2)))
            .replace("13_TEMPLATE_ORDER_TIME", dtFormatter.format(ZonedDateTime.now()));
    }
 */

    private static String getCreditCardExpiry() {
        int month = rng.nextInt(12) + 1;
        int year = Year.now().getValue() + 1 + rng.nextInt(3) - 2000;
        return (month < 10 ? "0" : "") + month + "/" + year;
    }
}
