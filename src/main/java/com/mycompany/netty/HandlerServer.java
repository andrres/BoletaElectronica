/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.EventExecutor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.DoubleStream.builder;
import static java.util.stream.IntStream.builder;
import static java.util.stream.Stream.builder;
import javax.net.ssl.HttpsURLConnection;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.json.*;

/**
 *
 * @author Abustos
 */
public class HandlerServer extends ChannelInboundHandlerAdapter {
    private static Instant INICIO = Instant.now();
    EventExecutor executor;
    ChannelGroup channels = new DefaultChannelGroup(executor);
    List<String> labels = Arrays.asList("GE_TIPO_OPERACION", "GE_TIPO_DTE", "GE_FECHA_EMI",
            "GE_RUT_EMI", "GE_RAZON_SOCIAL", "GE_NOM_ITEM", "GE_MONTO", "GE_ID_UNICO", "GE_MEDIO_PAGO",
            "GE_NOM_FANTASIA", "GE_DIR_COM", "GE_CIUDAD_COM", "GE_COD_COMERCIO", "GE_VER_POS",
            "GE_FECHA_OPERACION", "GE_HORA_OPERACION", "GE_TERMINAL", "GE_NUM_TARJETA",
            "GE_FORMA_OPERACION", "GE_PROPINA", "GE_VUELTO", "GE_TOTAL", "GE_EMPLEADO", "GE_NUM_OPERACION",
            "GE_COD_AUTORIZACION", "GE_TASA_INTERES", "GE_GLOSA_CUOTA", "GE_GLOSA_CUOTA_2",
            "GE_GLOSA_PROMOCION", "GE_TIPO_MONEDA", "GE_SALDO_PREPAGO", "GE_GLOSA_PREPAGO",
            "GE_FECHA_CONTABLE", "GE_NUM_CUENTA", "GE_TIPO_CUOTA", "GE_VAL_CUOTA_1", "GE_VAL_CUOTA_2",
            "GE_VAL_CUOTA_3", "GE_VAL_CUOTA_DIF_1", "GE_TASA_DIF_2", "GE_VAL_CUOTA_DIF_2", "GE_TASA_DIF_3",
            "GE_VAL_CUOTA_DIF_3", "GE_NUM_CUOTAS", "GE_CANAL", "GE_GLOSA_SURCHARGE", "GE_LABEL_ID",
            "GE_APLICATION_ID", "GE_RUT_RECEPTOR", "GE_NOMBRE_RECEPTOR", "GE_DIRECCION_RECEPTOR",
            "GE_COMUNA_RECEPTOR", "GE_CIUDAD_RECEPTOR", "GE_EMAIL_RECEPTOR", "GE_Monto_Neto", "GE_Monto_IVA");
    Map<String, String> elementsJson = new HashMap();

    //leer lo que llegue de info
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        INICIO = Instant.now();
        Log("Inicio ChannelRead"+ ctx.name());
        ByteBuf inBuffer = (ByteBuf) msg;
        String received = inBuffer.toString(CharsetUtil.UTF_8);
//        System.out.println("Rq Cliente :");
//        System.out.println(received + "-----------");
        parseData(received);
        if (elementsJson.get("GE_RUT_EMI").equals("") || elementsJson.get("GE_RAZON_SOCIAL").equals("")) {
            completeData(elementsJson.get("GE_COD_COMERCIO"));
        }
        String json = toJson();
        String Resp = sendRequest("http://bbn.ext.apigw.desarrollo.transbank.local:11030/servicios/creaBoleta", json, "Basic dHJhbnNiYW5rOmFjZXB0YTIwMjA=", "application/json; charset=UTF-8", null);
//        String Resp = sendRequest("http://localhost:8082/servicios/creaBoleta", json, "Basic dHJhbnNiYW5rOmFjZXB0YTIwMjA=", "application/json; charset=UTF-8", null);
//        System.out.println("Respuest ACEPTA");
//        System.out.println(Resp);
        String tramaFinal = encodResponse(Resp);
//        Este fireChannelRead llama al siguiente channerl handler en el pipeline
//        ctx.fireChannelRead("");
        Log("Fin ChannelRead"+ctx.name());
        ctx.write(Unpooled.copiedBuffer(tramaFinal, CharsetUtil.UTF_8));
    }

    
    //cuando ya no hay mas info
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
        System.out.println("channelReadComplete");
    }

    //el error en los anteriores enviando o recibiendo 
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
//todo esta puede ir en una clase utils.

    public void parseData(String dataRequest) {
        String aux = dataRequest.substring(4);
        String largo = dataRequest.substring(0, 4);
//        System.out.println("Largo Trama: " + largo);
//        System.out.println(aux);
        String operacion = aux.substring(2, 3);
//        System.out.println("Tipo Operacion: " + operacion);
        aux = aux.substring(3);
        int i = 0;
        String key = labels.get(i);
        elementsJson.put(key, operacion);
        while (aux.length() != 0 && i < 55) {
            i++;
            key = labels.get(i);
            int index = Integer.parseInt(aux.substring(0, 2));
//            System.out.println("Index :" + index);
            aux = aux.substring(2);
            String campo = aux.substring(0, index);
            elementsJson.put(key, campo);
//            System.out.println("Campo Data: " + campo);
            aux = aux.substring(index);
//            System.out.println("Trama :" + aux);
        }
    }

    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(elementsJson);
//            System.out.println("JSON Final: ");
//            System.out.println(json);
            return json;
        } catch (JsonProcessingException ex) {
            Logger.getLogger(HandlerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String sendRequest(String urlRq, String bodyRequest, String AuthBasic, String ctype, String SoapAction) throws MalformedURLException, IOException, NoSuchAlgorithmException, KeyManagementException {
        URL url = new URL(urlRq);
        URLConnection connection = url.openConnection();
        HttpURLConnection conn = (HttpURLConnection) connection;
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", AuthBasic);
        conn.setRequestProperty("X-API-Key", "ZHRlYm9sZXRhLmR0ZWJvbGV0YTozYWRiZWQ0YWY5NDU1MjZiMGI3MjAxMGYxMWNlZDU3NGUzNmRmNTBmZWMyZDE3ODdiNDU4Y2I0MTg2OTc5ZTNj");
        conn.setRequestProperty("Content-Type", ctype);
        conn.setRequestProperty("SOAPAction", SoapAction);
        conn.getOutputStream().write(bodyRequest.getBytes());
        String response = readResponse(conn);
        return response;
    }

    public String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getInputStream();
        int x;
        StringBuffer sb = new StringBuffer();
        while ((x = is.read()) != -1) {
            sb.append((char) x);
        }
        is.close();
        return sb.toString();
    }

    public void completeData(String codigoComercio) throws IOException, NoSuchAlgorithmException, MalformedURLException, KeyManagementException, ParserConfigurationException, SAXException {
        String body = bodyComercio(codigoComercio);
        String resp = sendRequest("http://172.30.145.42:7800/Integracion/Comercio", body, "", "text/xml;charset=UTF-8", "\"http://www.transbank.cl/entidad/comercio/v1/obtenerResumenComercio\"");
//        String resp = sendRequest("http://localhost:8081/Integracion/Comercio", body, "", "text/xml;charset=UTF-8", "http://www.transbank.cl/entidad/comercio/v1/obtenerResumenComercio");
//        System.out.println("Resp Comercio");
//        System.out.println(resp);
        Document doc = convertStringToXMLDocument(resp);

    }

    public String bodyComercio(String codigo) {
        String rqComercio = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v1=\"http://www.transbank.cl/entidad/comercio/v1\" xmlns:enc=\"http://www.transbank.cl/EncabezadoPortal\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <v1:obtenerResumenComercioRequest>\n"
                + "         <enc:encabezado>\n"
                + "            <enc:canal>?</enc:canal>\n"
                + "            <enc:ipCanal>?</enc:ipCanal>\n"
                + "            <enc:usuario>?</enc:usuario>\n"
                + "            <!--Optional:-->\n"
                + "            <enc:ipUsuario>?</enc:ipUsuario>\n"
                + "            <!--Optional:-->\n"
                + "            <enc:aplicacion>?</enc:aplicacion>\n"
                + "            <enc:idTransaccionCanal>?</enc:idTransaccionCanal>\n"
                + "            <!--Optional:-->\n"
                + "            <enc:idTransaccionMDW>?</enc:idTransaccionMDW>\n"
                + "            <enc:espacioNombre>?</enc:espacioNombre>\n"
                + "            <enc:servicio>?</enc:servicio>\n"
                + "            <enc:operacion>?</enc:operacion>\n"
                + "            <!--Optional:-->\n"
                + "         </enc:encabezado>\n"
                + "         <v1:cuerpo>\n"
                + "            <v1:codigoComercio>" + codigo + "</v1:codigoComercio>\n"
                + "         </v1:cuerpo>\n"
                + "      </v1:obtenerResumenComercioRequest>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        return rqComercio;
    }

    private Document convertStringToXMLDocument(String xmlString) {
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //API to obtain DOM Document instance
        DocumentBuilder builder = null;
        try {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();
            //Parse the content to Document object
            Document doc = (Document) builder.parse(new InputSource(new StringReader(xmlString)));
            String xpagResponse = "//*[local-name()='codigoRespuesta']/text()";
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xPath.compile(xpagResponse);
            String estadoRespues = (String) expr.evaluate(doc, XPathConstants.STRING);
            if (Integer.parseInt(estadoRespues) == 0) {
                String xpathRZ = "//*[local-name()='razonSocial']/text()";
                String xpathrut = "//*[local-name()='rutOrg']/text()";
                expr = xPath.compile(xpathRZ);
                String razon = (String) expr.evaluate(doc, XPathConstants.STRING);
                //falta conevertirla en base64
                expr = xPath.compile(xpathrut);
                String rut = (String) expr.evaluate(doc, XPathConstants.STRING);
//                System.out.println(" RUT y Razon :" + razon + " -- " + rut);
                if (elementsJson.get("GE_RUT_EMI").equals("")) {
                    elementsJson.put("GE_RUT_EMI", rut);
                }
                if (elementsJson.get("GE_RAZON_SOCIAL").equals("")) {
                    String textEncoded = new String(Base64.getEncoder().encodeToString(razon.getBytes()).getBytes("UTF-8"));
                    elementsJson.put("GE_RAZON_SOCIAL", textEncoded);
                }
            }
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String encodResponse(String jsonResp) {
        JSONObject obj = new JSONObject(jsonResp);
        String tramaFinal = "";
        String aux = obj.getString("GS_CODIGO_RESPUESTA");
        if (!aux.isEmpty()) {
            tramaFinal = tramaFinal + "0" + aux.length() + "" + aux;
        } else {
            tramaFinal = "00" + tramaFinal;
        }
        aux = obj.getString("GS_MENSAJE_RESPUESTA");
        if (!aux.isEmpty()) {
            tramaFinal = tramaFinal + "" + aux.length() + "" + aux;
        } else {
            tramaFinal = "00" + tramaFinal;
        }
        aux = obj.getString("GS_NUMERO_FOLIO");
        if (!aux.isEmpty()) {
            int l = aux.length();
            if (l < 10) {
                tramaFinal = tramaFinal + "0" + aux.length() + "" + aux;
            } else {
                tramaFinal = tramaFinal + "" + aux.length() + "" + aux;
            }
        } else {
            tramaFinal = "00" + tramaFinal;
        }
        aux = obj.getString("GS_ID_UNICO_TBK");
        if (!aux.isEmpty()) {
            tramaFinal = tramaFinal + "" + aux.length() + "" + aux;
        } else {
            tramaFinal = "00" + tramaFinal;
        }
        aux = obj.getString("GS_TED");
        if (!aux.isEmpty()) {
            tramaFinal = tramaFinal + "" + aux.length() + "" + aux;
        } else {
            tramaFinal = "00" + tramaFinal;
        }
        int largp = tramaFinal.length();
        if (largp < 100) {
            tramaFinal = "00" + largp + tramaFinal;
        }
        if (largp > 100 && largp < 1000) {
            tramaFinal = "0" + largp + tramaFinal;
        }
        if (largp > 1000) {
            tramaFinal = largp + "" + tramaFinal;
        }

        return tramaFinal;
    }
    
    private static void Log(Object mensaje) {
        System.out.println(String.format("%s [%s] %s",
                Duration.between(INICIO, Instant.now()), Thread.currentThread().getName(), mensaje.toString()));
    }

}
