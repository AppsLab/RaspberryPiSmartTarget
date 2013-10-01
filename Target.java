import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.OutputStream;
import be.doubleyouit.raspberry.gpio.Boardpin;
import be.doubleyouit.raspberry.gpio.Direction;
import be.doubleyouit.raspberry.gpio.GpioGateway;
import be.doubleyouit.raspberry.gpio.impl.GpioGatewayImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.activation.MimetypesFileTypeMap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.util.Random;

public class Target {
    static OutputStream output;
    static GpioGateway gpio;
    static SerialPort port;
    static CommPortIdentifier portId;

    static HttpClient httpclient = new DefaultHttpClient(); 
    static String replyURL;
    static String message;
    static boolean ready = true;
    static String dropcam = "https://nexusapi.dropcam.com/get_image?width=800&uuid=XXXX";
    static String osnImg;
    public static Date now;
    static Random generator = new Random();
    
    public static String[] messages = { 
	"Lucky! Looks like you will be taking me home tonight!",
	"It paid off to stop by the OTN Lounge tonight.",
	"Looks like someone has been practicing.",
	"Not bad for your first try. Now go ahead and brag to your friends.",
	"I love hanging out at the OTN Lounge, but It looks like you will be taking me home tonight!",
	"That was nice. No go and tell your friends to stop by the OTN Lounge.",
	"I just love to scream and fly at the OTN Lounge.", 
	"Roses are red and Oracle too. Now you can take me home too.",
	"I just wanna fly.",
	"Thanks for stopping by. Looks like you got lucky tonight.",
	"I just love to scream and fly at the OTN Lounge."
	};

    public static void main(String[] args) throws Exception{
        AddShutdownHookSample kill = new AddShutdownHookSample();
        kill.attachShutDownHook();
        

        try{
            portId = CommPortIdentifier.getPortIdentifier("/dev/ttyS1");
            port = (SerialPort)portId.open("Raspi LCD", 4000);
            output = port.getOutputStream();
            port.setSerialPortParams(9600,SerialPort.DATABITS_8, SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
            

            gpio = new GpioGatewayImpl();
            gpio.unexport(Boardpin.PIN11_GPIO17);
            gpio.export(Boardpin.PIN11_GPIO17);
            gpio.setDirection(Boardpin.PIN11_GPIO17, Direction.IN);
	    
            while (true){

        	    brightness("9");
        	    clear();
        	    print("     Ready      ");
        	    delayBacklight("0",5);
        	    int i = 0;
		//remove negation!
                while (!gpio.getValue(Boardpin.PIN11_GPIO17)){
                   i = 1; 
                }

                if (ready){
                    //System.out.println("start");
                    ready = false;
                    int rnd = generator.nextInt(10);
                    Process proc = Runtime.getRuntime().exec(new String[]{"/usr/bin/mpg321", "-q","/home/pi/"+rnd+".mp3"});
                    brightness("9");
                    clear();
                    print(" Congratulations");
                    //Login
                    downloadPicture();
                    uploadPicture(messages[rnd]);	

                    clear();
                    delayBacklight("0",5);
                    Thread.sleep(2 * 5000);
                    ready = true;
                    //System.out.println("end");
                }

            }    
        }catch (Exception e){
            System.out.println(e.toString());
        }
    }




    public static void downloadPicture() throws IOException{
        now = new Date();
        String formatDate = new SimpleDateFormat("MMddyyyy-hhmmss").format(now);
        osnImg = "OTN-" + formatDate + ".jpeg";
        
        HttpGet httpget = new HttpGet(dropcam);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        //System.out.println("downloadPicture: " +response.getStatusLine());
        if (entity != null) {

            byte[] bytes = EntityUtils.toByteArray(entity);
            File file = new File("/var/www/" + osnImg);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();     
               
        }    
        EntityUtils.consume(entity);
        
    }    

    public static void uploadPicture(String message) throws TwitterException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setOAuthConsumerKey("XXXX");
        cb.setOAuthConsumerSecret("XXXX");
        cb.setOAuthAccessToken("XXXX");
        cb.setOAuthAccessTokenSecret("XXXX");

        StatusUpdate status = new StatusUpdate(message);

        File imageFile = new File ("/var/www/" +  osnImg); 
        status.setMedia(imageFile);
                
        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();
            
                
        try {
            twitter.updateStatus(status);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

    }


    public static void delayBacklight(String level, int delay ) throws Exception{
        startCommand();
        output.write("T".getBytes());
        output.write(level.getBytes());
        output.write(delay);
    }

    public static void brightness(String level) throws Exception{
        startCommand();
        output.write("B".getBytes());
        output.write(level.getBytes());
    }

    public static void print(String message) throws Exception{
        output.write(message.getBytes());
    }

    public static void clear() throws Exception{
        startCommand();
        output.write("C".getBytes());
    }

    public static void startCommand() throws Exception{
        output.write(254);
    }

}

//http://www.javabeat.net/2010/11/runtime-addshutdownhook/
class AddShutdownHookSample {
  void attachShutDownHook() {
  Runtime.getRuntime().addShutdownHook(new Thread() {
   @Override
   public void run(){
    try{
        System.out.println("Shuting down target");
        File file = new File("/var/lock/LCK..ttyS1");
        Lcd.httpclient.getConnectionManager().shutdown();
        if(file.delete()){
            System.out.println(file.getName() + " is deleted!");
        }
    }catch(Exception e){
        e.printStackTrace();
    }
   }
  });
    //System.out.println("Shut Down Hook Attached.");
 }
}
