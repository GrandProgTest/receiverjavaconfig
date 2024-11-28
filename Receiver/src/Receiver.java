import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import javax.jms.MapMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;

public class Receiver {

	public static void main(String args[]) throws Exception {
		Properties env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		env.put(Context.PROVIDER_URL, "tcp://localhost:61616");
		env.put("queue.queueSampleQueue", "test");

		// Obtener el contexto inicial
		InitialContext ctx = new InitialContext(env);

		// Buscar la cola
		Queue queue = (Queue) ctx.lookup("queueSampleQueue");

		// Buscar la fábrica de conexiones
		QueueConnectionFactory connFactory = (QueueConnectionFactory) ctx.lookup("QueueConnectionFactory");

		// Crear una conexión
		QueueConnection queueConn = connFactory.createQueueConnection();

		// Crear una sesión
		QueueSession queueSession = queueConn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

		// Crear un receptor
		QueueReceiver queueReceiver = queueSession.createReceiver(queue);

		// Iniciar la conexión
		queueConn.start();

		// Recibir un mensaje
		MapMessage message = (MapMessage) queueReceiver.receive();

		// Procesar el mensaje recibido
		String nombre = message.getString("nombre");
		String email = message.getString("email");
		System.out.println("Recibido: " + nombre + " | " + email);

		// Crear un objeto JSON para enviar al servicio REST
		String json = String.format("{\"nombre\": \"%s\", \"email\": \"%s\"}", nombre, email);

		// Enviar el mensaje al servicio REST
		sendToRestService(json);

		// Cerrar la conexión
		queueConn.close();
	}

	// Método para enviar datos al servicio REST
	private static void sendToRestService(String json) {
		try {
			// URL del endpoint REST
			URL url = new URL("http://127.0.0.1:8080/person");

			// Abrir conexión
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			// Escribir el cuerpo de la solicitud
			OutputStream os = conn.getOutputStream();
			os.write(json.getBytes());
			os.flush();

			// Verificar la respuesta
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_CREATED) {
				System.out.println("Cliente agregado correctamente al servicio REST.");
			} else {
				System.out.println("Error al enviar al servicio REST. Código de respuesta: " + responseCode);
			}

			// Cerrar la conexión
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
