package tp2.repartiteur;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tp2.shared.ServeurInterface;

/**
 * Classe principale du répartiteur en mode sécurisé
 * @author Enzo Laurent / Victor Pongnian 
 */
public class Repartiteur {

	private List<ServeurInterface> listeServeurInterface;               // Liste des serveurs que le répartiteur va utilisé
	private List<String> listeNomServeur = new ArrayList<String>();     // Liste des noms des serveurs.
	private List<Thread> listeServeurThread = new ArrayList<Thread>();  // Liste des Thread utilisés
	private static HashMap<String, ArrayList<Integer>> listeOperations; // Liste des opérations à traiter 
	private static int nbrOperations = 0;                               // Compteur d'opérations en cours. 
	private static int nbrOperationsInitial = 0;						// Initialisé pendant l'appel de parseListeOperations 
	private int resultatFinal = 0;										// Résultat final
	private int nbrServeurConnecte = 0;									// Nombre de serveur disponible pour effectuer le calcul
	private int nbrServeurInitial = 0;									// Nombre de serveur initialement disponible lors du lancement du répartiteur
	private int nbrServeurErreur = 0;									// Nombre de serveur en erreur (déconnecté)
	private int nbrServeurOccupe = 0;									// Nombre de serveur occupé (à faire un calcul)
	private long tempsDebut = 0;										// Temps début du calcul
	private long tempsFin = 0;											// Temps de fin de calcul.
	
	/**
	 * Constructeur du répartiteur. Va parser le fichier de configuration et
	 *  lancer les threads pour chaque serveur
	 *  @param config_file_path fichier de configuration
	 *  
	 *  */
	public Repartiteur(String config_file_path) {
		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		//Initialisation de la liste des serveurs distants
		this.listeServeurInterface = new ArrayList<ServeurInterface>();
		
		//Initialisation du compteur pour savoir le nombre de serveur chargé.
		int i = 1;
		
		//Analyse du fichier de configuration
		try {
			BufferedReader buff = new BufferedReader( new FileReader(config_file_path));
			
			try {
				String line;
				String[] temp;
				
				boolean err = false;
				
				while((line=buff.readLine()) != null){
					temp = line.split(" ");
					int code_retour = loadConfigServeur(temp); //Chargement du stub avec loadConfigServeur
					
					switch(code_retour) {
						case 0: 
							break;
						case -1:
							err = true;
							System.out.println("Erreur dans le format du nom du " + i + "eme serveur. Caractère interdit");
							break;
						case -2:
							err = true;
							System.out.println("Erreur dans le format de la " + i + "eme adresse. Veuillez rentrer une adresse IP valide");
							break;
						case -3:
							err = true;
							System.out.println("Erreur de format pour le " + i + "eme port. Le port doit être compris entre 5000 et 5050");
							break;
						default:
							err = true;
							System.out.println("Erreur inconnue");
							break;
					}
					//Si il y une erreur dans un code de retour, on arrete le chargement des serveurs
					if(err){
						break;
					}
					i++;
				}
			} finally {
				buff.close();
			}
		} catch (IOException e) {
			System.out.println("Erreur: le fichier de config n'a pas pu être trouvé");
		} 
		
		//Après avoir chargé tous les stubs des serveurs, on crée un thread pour chaque serveur disponible.
		for (int j = 0; j < i-1; j++){	
			//On crée une instance de RunServeurImpl pour chaque serveur. 
			//C'est cette classe qui aura la responsabilité de gérer la 
			//reception des résultats du serveur auquel elle est rattachée.
			this.listeServeurThread.add(new Thread(new RunServeurImpl(listeNomServeur.get(j), listeServeurInterface.get(j), this)));
			nbrServeurInitial++;
		}
	}
	
	/**
	 * Lancement du répartiteur. C'est le thread principal qui va s'occuper de lancer 
	 * tous les threads correspondant aux serveurs.
	 */
	private void run() {
		
		tempsDebut = System.nanoTime();  //Démarrage du timer de début de calcul
		for (Thread thread : this.listeServeurThread){
			thread.start();
			nbrServeurConnecte++;
		}
		
		// Tant qu'il existe un serveur connecté (voir condition dans les threads) 
		// c'est qu'il y a toujours des calculs à faire et donc on bloque le thread principal.
		while(nbrServeurConnecte !=0){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
		}
		
		tempsFin = System.nanoTime();   //Fin du calcul
		
		// Si tous les serveurs ont été déconnecté (serveur en erreur) lors la phase de 
		// calcul, il est alors impossible qu'on soit parvenu au résultat
		// final.
		if(nbrServeurInitial != nbrServeurErreur)
		{
			System.out.println("Resultat : " + resultatFinal);
			System.out.println("Temps necessaire au calcul : " + (tempsFin - tempsDebut));
		}
		else
		{
			System.out.println("Erreur: défaillance avec tous les serveurs, pas de résultat");
		}
		
	}
	/**
	 * Méthode de chargement des stubs.
	 * 
	 * @param hostname addresse du rmiregistry
	 * @param port port de connexion du rmiregistry
	 * @param nomServeur nom du service enregistré dans le rmiregistry
	 * @return stub de type ServeurInterface
	 */
	private ServeurInterface loadServerStub(String hostname, int port, String nomServeur) {
		ServeurInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname, port);
			stub = (ServeurInterface) registry.lookup(nomServeur);
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}
	
	/**
	 * Parse du fichier de données à calculer. On récupère toutes les opérations 
	 * dans une stucture type table de hachage avec caractéristiques suivantes.
	 * 
	 * Key : Nom de l'opérateur (Ici correspond à la méthode)
	 * Value : Liste de tous les opérandes. 
	 * De cette manière on pourrait imaginer pouvoir stocker n'importe quel type de calcul
	 * simple de la forme Opérateur/Opérandes.
	 * 
	 * @param cheminFichierOperations chemin du fichier de configuration.
	 * @return Table de hachage contenant toutes les opérations à effectuer
	 * 
	 */
	private static HashMap<String, ArrayList<Integer>> parseListeOperations(String cheminFichierOperations){
		
		HashMap<String, ArrayList<Integer>> list = new HashMap<String, ArrayList<Integer>>();
			
		try {
			BufferedReader buff = new BufferedReader( new FileReader(cheminFichierOperations));
			
			try {
				String line;
				String[] temp;
				
				while((line=buff.readLine()) != null){
					temp = line.split(" ");
					if(!list.containsKey(temp[0])){
						list.put(temp[0], new ArrayList<Integer>());
					}
					list.get(temp[0]).add(Integer.parseInt(temp[1]));
					nbrOperations++;
					nbrOperationsInitial++;
				}
			} finally {
				buff.close();
			}
		} catch (IOException e) {
			System.out.println("Erreur: " + e.getMessage());
		} 
		
		return list;
	}
	
	/**
	 * 
	 * @param temp Tableau contenant les paramètres suivants:
	 * - Nom du service à obtenir auprès du rmiregistry 
	 * - addresse et port du rmiregistry.
	 * 
	 * @return code de retour
	 */
	private int loadConfigServeur(String[] temp){
		if(isServeurFormat(temp[0])){
			if(isAdresseFormat(temp[1])){
				if(isPortFormat(temp[2])){
					listeServeurInterface.add(this.loadServerStub(temp[1],Integer.parseInt(temp[2]),temp[0]));
					listeNomServeur.add(temp[0]);
				} else {
					return -3;
				}
			} else{
				return -2;
			}
		} else {
			return -1;
		}
		return 0;
	}
	
	/**
	 * vérifie si la chaine d'entrée ressemble à un nom de service
	 * @param s chaine à analyser
	 * @return boolean 
	 */
	private static boolean isServeurFormat(String s){
		Pattern p = Pattern .compile("^[a-zA-Z0-9]*$");
	    Matcher m = p.matcher(s);
	
		return m.matches();
	}
	
	/**
	 * vérifie si la chaine d'entrée ressemble à une adresse IP (Pas complètement)
	 * @param s chaine à analyser
	 * @return boolean 
	 */
	private static boolean isAdresseFormat(String s){
		Pattern p = Pattern .compile("^^([0-9]{1,3}\\.){3}[0-9]{1,3}$");
	    Matcher m = p.matcher(s);
	
		return m.matches();
	}
	
	/**
	 * vérifie si la chaine d'entrée ressemble à un port compris entre 5000 et 5050
	 * @param s chaine à analyser
	 * @return boolean 
	 */
	private static boolean isPortFormat(String s){
	
		int port = Integer.valueOf(s);
		
		if(port >= 5000 && port <= 5050)	
			return true;
		else
			return false;
	}
	
	/**
	 * Méthode principal. Vérifie le nombre d'argument, initialise le répartiteur, récupère la liste d'opération et 
	 * lance la méthode run.
	 * @param args donnees_a_calculer + fichier de configuration du répartiteur.
	 */
	public static void main(String[] args) {
		
		if (args.length == 0){
			System.out.println("Veuillez rentrer un nom de fichier contenant les operations à faire");
		} else if (args.length == 1){
			System.out.println("Veuillez rentrer un fichier de configuration");
		} else {
			//Initialisation du répartiteur. On donne le fichier de configuration en paramètre.
			Repartiteur repartiteur = new Repartiteur(args[1]);
			
			listeOperations = parseListeOperations(args[0]);
		
			repartiteur.run();
		}
	
	}
	
	/**
	 * 
	 * @return int nombre d'Operation
	 */
	public synchronized int getNbrOperations() {
		return nbrOperations;
	}
	
	/**
	 * getter sur nbrOperationsInitial. 
	 * @return int nombre d'Operation
	 */
	public synchronized int getNbrOperationsInitial() {
		return nbrOperationsInitial;
	}
	
	/**
	 * Décrémente la valeur de nbrServeurConnecte car un thread a été déconnecté. 
	 */
	public synchronized void deconnectThread() {
		nbrServeurConnecte--;
	}
	
	/**
	 * Ajoute un résultat intermédiaire au résultat final. Méthode appelée depuis un thread
	 * @param resultat
	 */
	public synchronized void setResultat(int resultat) {
		nbrServeurOccupe--;
		resultatFinal = (resultatFinal + resultat) % 5000;
	}
	
	/**
	 * Méthode visant à transéferer des opérations à faire calculer à un serveur
	 * dans une structure similaire à la listeOperations du répartiteur.
	 * 
	 * @param i nombre d'opération a obtenir.
	 * @return liste HashMap<String, ArrayList<Integer>> contenant les opérations obtenus depuis listeOperations.
	 */
	public synchronized HashMap<String, ArrayList<Integer>> getOperations(int i){
		
		HashMap<String, ArrayList<Integer>> listeOp = new HashMap<String, ArrayList<Integer>>();
		int compteur = 0;
		boolean outLoop = false;
		
		//On parcours à priori toutes les clés
		for(String currentKey : listeOperations.keySet()) {
			//Utilisation d'un iterator pour pouvoir enlever des éléments en même temps qu'on parcout la liste.
			Iterator<Integer> transferElement = listeOperations.get(currentKey).iterator();
			
			//On boucle tant qu'il existe un élément suivant dans la liste des opérandes liés à une opération 
			while(transferElement.hasNext()){
				if(compteur < i ){ 
					if(!listeOp.containsKey(currentKey)){                      
						listeOp.put(currentKey, new ArrayList<Integer>());
					}
					listeOp.get(currentKey).add((Integer) transferElement.next());
					transferElement.remove();
					
					compteur++;
					nbrOperations--;
				} else {  //Si on le bon nombre d'opération dans listeOperations, alors on sort de la boucle.   
					outLoop = true;
					break;
				}
			}
			if(outLoop){
				break; //Arrive seulement si est passé dans le else précédent.
			}
		}
		nbrServeurOccupe++;
		return listeOp;
	}
	
	/**
	 * Méthode visant à remettre des opérations dans la listeOperations général du répartiteur.
	 * Peut arriver lorsqu'un serveur distant s'est déconnecté. dans ce cas ces opérations
	 * doivent être redistribuées.
	 * 
	 * @param operations liste des opérations à remettre dans listeOperations
	 * @param erreur si un serveur s'est déconnecté.
	 */
	public synchronized void resetOperations(HashMap<String, ArrayList<Integer>> operations, boolean erreur) 
	{
		for(String s : operations.keySet()){
			for(Integer i : operations.get(s)){
				listeOperations.get(s).add(i);
				nbrOperations++;
			}
		}
		
		nbrServeurOccupe--;
		
		if(erreur)
		{
			nbrServeurErreur++;
		}
	}
	
	/**
	 * getter sur nbrServeurOccupe depuis les threads.
	 * @return nbrServeurOccupe
	 */
	public synchronized int getNbrServeurOccupe() {
		return nbrServeurOccupe;
	}
	
	/**
	 * getter sur nbrServeurInitial depuis les threads.
	 * @return nbrServeurInitial
	 */
	public synchronized int getNbrServeurInitial() {
		return nbrServeurInitial;
	}
}

