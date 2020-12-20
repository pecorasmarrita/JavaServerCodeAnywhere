package mainpackage;

public class Car {

	String marca;
	String modello;
	
	public void setMarca (String marca)
	{
		this.marca = marca;
	}
	
	public void setModello (String modello)
	{
		this.modello = modello;
	}
	
	public String getMarca ()
	{
		return marca;
		
	}
	
	public String getModello ()
	{
		return modello;
		
	}

	@Override
	public String toString() {
		return "Car [marca=" + marca + ", modello=" + modello + "]";
	}
	
}
