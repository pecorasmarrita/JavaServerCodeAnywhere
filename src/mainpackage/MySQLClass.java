package mainpackage;

import java.sql.*;

public class MySQLClass 
{

	public static void main(String[] args)
    {
		Car car = new Car(); // nuova istanza oggetto car
        String mySQLDriver = "com.mysql.cj.jdbc.Driver"; // selezione driver aggiornato
        
        try
        {
            Class.forName(mySQLDriver);
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Driver not found " + e);
            System.exit(0);
        }
        // jdbc:mysql://indirizzo/schema?[args]
        String url_db = "jdbc:mysql://localhost:3306/cars?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        // query per marca e modello dalla table car_table
        String query = "SELECT marca, modello FROM car_table";
        System.out.println("Connettendo presso: " + url_db); // output per tentativo di connessione con db
        Connection connection = null; // inizializzazione variabile
        
        try
        {
            connection = DriverManager.getConnection(url_db, "root", "root"); // connessione a url tramite user e password
        }
        catch (Exception e)
        {
            System.out.println("Errore durante la connessione: " + e);
            System.exit(0);
        }
        
        try
        {
            Statement statement = connection.createStatement(); // creazione statement
            ResultSet resultset = statement.executeQuery(query); // query presso database
            while(resultset.next())
            {
            	car.setMarca (resultset.getString(1));
            	car.setModello (resultset.getString(2));
                System.out.println("INFORMAZIONI");
                System.out.println("Marca: " + car.getMarca());
                System.out.println("Modello: " + car.getModello());
            }
        }
        catch(Exception e)
        {
            System.out.println("Eccezione: " + e);
            System.exit(0);
        }
        finally
        {
            if(connection != null)
            {
                try
                {
                    connection.close();
                }
                catch(Exception e)
                {
                    System.out.println("Errore durante chiusura connessione: " + e);
                }
            }
        }
    }
	
}
