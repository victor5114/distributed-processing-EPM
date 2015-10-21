package tp2.serveur;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import tp2.serveur.Operations;
import tp2.shared.ServeurInterface;

/**
 * Classe du serveur. Service offert à distance.
 * @author Enzo Laurent / Victor Pongnian
 *
 */
public class Serveur implements ServeurInterface  {
	
	private String nomServeur;    // Nom du serveur
	private int seuilErreur;      // Seuil de malice. (Testé avec 50%)
	private int Q_OP_MAX;         // Nombre d'opération maximum acceptées.
	
	/**
	 * Constructeur du serveur. Hydrate simplement les attributs
	 * @param nomServeur
	 * @param seuilErreur
	 * @param nbrOperationMax
	 */
	public Serveur(String nomServeur, int seuilErreur, int nbrOperationMax){
		super();
		this.nomServeur = nomServeur;
		this.seuilErreur = seuilErreur;
		this.Q_OP_MAX = nbrOperationMax;
	}
	
	/**
	 * Méthode principale du serveur. On récupère les variables en arguments :
	 * 
	 * @param args
	 * - nom du serveur
	 * - nomre d'operation max
	 * - seuil d'erreur
	 * - addresse du rmiregistry
	 * - port du rmiregistry
	 */
	public static void main(String[] args) {
		
		if(args.length == 5){
			String nomServeur = args[0];
			try {
				int nbrOperationMax = Integer.parseInt(args[1]);
				int seuilErreur = Integer.parseInt(args[2]);
				int port = Integer.parseInt(args[3]);
				
				Serveur serveur = new Serveur(nomServeur, seuilErreur, nbrOperationMax);
				serveur.run(port);
			} catch (NumberFormatException e){
				System.err.println("Erreur: " + e.getMessage());
			}

		} else {
			System.out.println("Arguments du serveur incorrects. Veuillez rentrer une commande de la forme : \n"
					+ "./serveur nomDuServeur nbrOperationMax seuilErreur adresseDuRegistry portDuRegistry");
		}
	}
	
	/**
	 * Méthode lancée par le main. 
	 * On exporte le service sur un port d'écoute.
	 * Ensuite on enregistre le service du serveur
	 * auprès du rmiregistry. 
	 * 
	 * @param hostname addresse du rmiregistry auquel on se rattache.
	 * @param port
	 */
	private void run(int port) {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServeurInterface stub = (ServeurInterface) UnicastRemoteObject
					.exportObject(this, 5011);     //Doit être modifié (valeur entre 5000 et 5050) si testé avec serveurs distants
                                                   //Possibilité de changer le port si ce dernier est déjà occupé 
			Registry registry = LocateRegistry.getRegistry("127.0.0.1",port);
			registry.rebind(this.nomServeur, stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	/**
	 * Méthode appelé à distance par le répartiteur (en réalité par les thread) pour le calcul du paquet
	 * @param operations Opérations envoyées par le répartiteur/
	 * @exception RemoteException Lance l'exception si erreur de connexion.
	 */
	public int calculOperations(HashMap<String, ArrayList<Integer>> operations) throws RemoteException {
		
		int sommeTotale = 0;  //Variable local servant au calcul du paquet d'opération
		int nbrOperations = 0;
		
		//On récupère le nombre d'opération contenu dans le paquet. 
		for(String s : operations.keySet()){
			nbrOperations = nbrOperations + operations.get(s).size();
		}
		
		
		Random rnd = new Random();
		int RndMalice = rnd.nextInt(100) + 1;   //Nombre aléatoire pour simuler la malice
		int RndResultFaux = rnd.nextInt(5001);  //Résultat faux généré au hasard (1/5000 pour avoir un bon résultat)
		
		//Si test si on peut accepter le résultat
		if(operationsAcceptees(nbrOperations))
		{
			System.out.println("Calcul en cours de " + nbrOperations + " opérations");
			
			//Parcours de la liste d'opération et calcul de chaque opérations.
			for(String s : operations.keySet()){
				if (s.equals("fib")){
					for(Integer i : operations.get(s)){
							sommeTotale = ((Operations.fib(i) % 5000) + sommeTotale) % 5000;
					}
				} else if (s.equals("prime")){
					for(Integer i : operations.get(s)){
							sommeTotale = ((Operations.prime(i) % 5000) + sommeTotale) % 5000;
					}
				} 
			}
			
			System.out.println("Calcul terminé");
			
			//Si on est en dessous du seuil d'erreur alors on renvoie un résultat faux !
			if(RndMalice <= seuilErreur)
			{
				return RndResultFaux;
			}
			//Sinon on renvoie le bon résultat.
			else 
				return sommeTotale;
			}
		//Sinon on retourne un code spécifique
		else 
		{
			return -1;
		}
	}
	
	/**
	 * Méthode qui permet de vérifier la disponibilité du serveur.
	 * @param nbrOperationsSoumises Paramètre servant à la simulation de la disponibilité du serveur.
	 * @return boolean Est ce qu'on accepte le calcul ou pas.
	 */
	private boolean operationsAcceptees(int nbrOperationsSoumises) {
		Random rnd = new Random();
		int RndRefus = rnd.nextInt(101);
		float tauxRefus;
		
		//Calcul du taux de refus suivant la formule suggérée
		tauxRefus = ((float) ( nbrOperationsSoumises - this.Q_OP_MAX ) / ( 9 * this.Q_OP_MAX )) * 100; 
		
		//Si tauxRefus négatif, on considère qu'on accepte tout le temps le calcul
		if (tauxRefus < 0){
			tauxRefus = 0;
		} 
		//Sinon on refuse tout le temps le calcul. 
		else if(tauxRefus > 100 )
		{
			tauxRefus = 100;
		}
		
		//Si on est en dessous du seuil de refus alors on accepte
		if(RndRefus >= (int) tauxRefus)
		{
			System.out.println("Calcul accepté");
			return true;
		}
		//Sinon on refuse le calcul.
		else
		{
			System.out.println("Calcul refusé");
			return false;
		}
	}

}
