package de.extra.extraClientLight.webservice;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 3.0.1
 * 2014-10-16T08:59:46.217+02:00
 * Generated source version: 3.0.1
 * 
 */
@WebServiceClient(name = "extra", 
                  wsdlLocation = "http://localhost/wsdl/extra.wsdl",
                  targetNamespace = "http://www.extra-standard.de/namespace/webservice") 
public class Extra_Service extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://www.extra-standard.de/namespace/webservice", "extra");
    public final static QName ExtraSOAP = new QName("http://www.extra-standard.de/namespace/webservice", "extraSOAP");
    static {
        URL url = null;
        try {
            url = new URL("http://localhost/wsdl/extra.wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(Extra_Service.class.getName())
                .log(java.util.logging.Level.INFO, 
                     "Can not initialize the default wsdl from {0}", "http://localhost/wsdl/extra.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public Extra_Service(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public Extra_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Extra_Service() {
        super(WSDL_LOCATION, SERVICE);
    }
    
    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public Extra_Service(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public Extra_Service(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public Extra_Service(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }    

    /**
     *
     * @return
     *     returns Extra
     */
    @WebEndpoint(name = "extraSOAP")
    public Extra getExtraSOAP() {
        return super.getPort(ExtraSOAP, Extra.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns Extra
     */
    @WebEndpoint(name = "extraSOAP")
    public Extra getExtraSOAP(WebServiceFeature... features) {
        return super.getPort(ExtraSOAP, Extra.class, features);
    }

}
