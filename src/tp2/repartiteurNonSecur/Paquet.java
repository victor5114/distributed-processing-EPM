package tp2.repartiteurNonSecur;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Enzo Laurent & Victor Pongnian
 * Class Paquet contenant les informations la liste des opérations et l'état du calcul 
 *
 */
public class Paquet {
	
	// Etat du calcul : vérifié ou non
	private boolean estVerifie;
	// Liste des serveurs ayant fait ce calcul
	private List<String> listeServeurTraite = new ArrayList<String>();
	// Liste des serveurs en train de faire le calcul
	private List<String> listeServeurEnCours = new ArrayList<String>();
	// Liste des résultats fournis par les serveurs avec le nombre de serveur ayant donné le même résultat
	private Map<Integer, Integer> listeResultat = new HashMap<Integer, Integer>();
	// Liste des opérations
	private HashMap<String, ArrayList<Integer>> listeOperation;	
	
	/**
	 * Constructeur de Paquet
	 * @param listeOperation
	 */
	public Paquet(HashMap<String, ArrayList<Integer>> listeOperation){
		this.estVerifie = false;
		this.listeOperation = listeOperation;
	}
	
	/**
	 * Retourne si le paquet est vérifié ou pas
	 * @return
	 */
	public synchronized boolean estVerifie() {
		return estVerifie;
	}
	
	/**
	 * Vérifie si le résultat est valide
	 * Si le résultat est valide, le résultat valide est renvoyé et estVerifie est mis à true
	 * Sinon -1 est renvoyé
	 * @param nbrServeurTotal, nombre de serveur connectés
	 * @return
	 */
	public synchronized int resultatValide(int nbrServeurTotal){
		
		int seuilValide = (int) Math.ceil((float) nbrServeurTotal / 2);
		int resultatValide = -1;
		
		for(int currentResultat : listeResultat.keySet())
		{
			// La condition !estVerifie est necessaire pour une histoire de concurrence
			if(listeResultat.get(currentResultat) >= seuilValide && !estVerifie)
			{
				estVerifie = true;
				resultatValide = currentResultat;
				break;
			}
		}
		
		return resultatValide;
	}
	
	/**
	 * Renvoie true si le nomServeur fourni en paramètre a déjà calculé ce paquet d'opération
	 * Sinon false
	 * @param nomServeur
	 * @return
	 */
	public synchronized boolean serveurDejaTraite(String nomServeur){
		
		boolean present = false;
		
		for (String serveur : listeServeurTraite){
			if(serveur == nomServeur)
			{
				present = true;
				break;
			}
		}
		
		return present;
	}
	
	/**
	 * Ajoute à la liste des résultats le résultat du serveur et le nom du serveur à la liste des serveurs ayant déjà fait le calcul
	 * Pour la liste des résultat :
	 * 	Si le résultat existe déjà dans le HashMap, le nombre de serveur ayant donné ce résultat est incrémenté, 
	 * 	Sinon il créé une clé avec un nouveau résultat
	 * @param nomServeur
	 * @param resultat
	 */
	public synchronized void setResultat(String nomServeur, int resultat){
		
		//On peut ajouter nomServeur dans listeServeurTraite car setResultat vient après l'appel de serveurDejaTraite
		listeServeurTraite.add(nomServeur);
		
		if(!listeResultat.containsKey(resultat)){
			listeResultat.put(resultat, 0);
		}
		
		listeResultat.put(resultat, listeResultat.get(resultat) + 1);
	}
	
	/**
	 * Ajoute un serveur à liste des serveurs en train de faire le calcul
	 * @param nomServeur
	 */
	public synchronized void setServeurEnCours(String nomServeur) {
		listeServeurEnCours.add(nomServeur);
	}
	
	/**
	 * Supprime un serveur à liste des serveurs en train de faire le calcul
	 * @param nomServeur
	 */
	public synchronized void delServeurEnCours(String nomServeur) {
		listeServeurEnCours.remove(nomServeur);
	}
	
	/**
	 * Retourne true si des serveurs sont en train de faire le calcul de ce paquet d'opération
	 * Sinon false
	 * @return
	 */
	public synchronized boolean hasServeursEnCours() {
		
		if(listeServeurEnCours.size() == 0)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	/**
	 * Retourne le liste d'opération de ce paquet
	 * @return
	 */
	public synchronized HashMap<String, ArrayList<Integer>> getListeOperation() {
		return listeOperation;
	}
	
	/**
	 * Retourne true si il y a plus de 50% des serveurs qui ont déjà ou qui sont en train de faire le calcul ET si des serveurs sont en train de faire le calcul
	 * Cela veut dire que ce paquet est potentiellement vérifiable, ca ne sert à rien qu'un serveur commence le calcul 
	 * @param nbrServeurTotal, nombre de serveur connectés
	 * @return
	 */
	public synchronized boolean verificationEnCours(int nbrServeurTotal) {
		if(((listeServeurEnCours.size() + listeServeurTraite.size()) > nbrServeurTotal/2) && hasServeursEnCours())
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
