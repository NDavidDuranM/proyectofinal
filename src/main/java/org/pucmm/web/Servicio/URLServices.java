package org.pucmm.web.Servicio;

import org.pucmm.web.Modelo.Cliente;
import org.pucmm.web.Modelo.URLs;
import org.pucmm.web.Modelo.Usuario;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class URLServices {

    private static URLServices instancia;
   // private HashMap<String, String> mapaClave; // mapa clave-url
    //private HashMap<String, String> mapaValor;//  mapa para validaciones

    private char caracteres[]; //Variable donde almacenaremos los 62 posibles caracteres de una URL
    private int longitud_url;
    private Set<URLs> urlsCliente;

    GestionDb gestionDb = new GestionDb(URLs.class);

    public URLServices()
    {
        urlsCliente = new HashSet<>();
        longitud_url = 6;

        String alfabeto = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int cantidad_letras = alfabeto.length();

        caracteres = new char[62];

        for(int i = 0 ; i < cantidad_letras; i++)
        {
            caracteres[i] = alfabeto.charAt(i);
        }

    }


    public static URLServices getInstance()
    {
        if(instancia == null)
        {
            instancia = new URLServices();
        }
        return instancia;
    }

    //Funcionar para retornar una URL acortada para ser utilizada
    private String acortarURL(String url)
    {
        EntityManager em = gestionDb.getEntityManager();

        try{
            String urlAcortada = "";
            url = formatearURL(url);

            if(gestionDb.find(url) != null)
            {
                url = em.find(URLs.class,url).getDireccionAcortada();
            }else
            {
                urlAcortada = getClave(url);
            }

            return urlAcortada;

        }finally {
            em.close();
        }
    }

    //Buscar en la base de datos la URL original y retornarla
    public String expandirURL(String urlAcortada)
    {
        URLs url = (URLs) gestionDb.find(urlAcortada);
        return url.getOrigen();
    }


    //Funcion para que la url escrita de varias maneras siga siendo valida
    private String formatearURL(String url)
    {
        if (url.startsWith("http://"))
            url = url.substring(7);

        if (url.startsWith("https://"))
            url = url.substring(8);

        if (url.charAt(url.length() - 1) == '/')
            url = url.substring(0, url.length() - 1);
        return url;
    }


    private String getClave(String longURL) {
        String clave = generarClave();
        return clave;
    }


    //Funcion para generar la clave de 6 digitos de la url
    private String generarClave() {

        Random rand = new Random();
        String clave = "";

        while (true) {
            clave = "";

            //Se va a ir creando una clave aleatorea
            for (int i = 1; i <= longitud_url; i++) {
                clave += caracteres[rand.nextInt(62)];
            }

            //Si la clave no se encuentra en la base de datos significa que no existe y que se puede usar, terminando asi la generacion
            if (gestionDb.find(clave) == null) {
                break;
            }
        }
        return clave;
    }

    public URLs nuevaUrlAcortada(String url) {
        URLs nuevaURL = new URLs();
        nuevaURL.setOrigen(url);
        nuevaURL.setDireccionAcortada(acortarURL(url));
        urlsCliente.add(nuevaURL);
        gestionDb.crear(nuevaURL);
        return nuevaURL;
    }

    public String crearRetornarUrlAcortada(String url)
    {
        URLs nuevaURL = new URLs();
        nuevaURL.setOrigen(url);
        nuevaURL.setDireccionAcortada(acortarURL(url));
        urlsCliente.add(nuevaURL);
        gestionDb.crear(nuevaURL);

        return nuevaURL.getDireccionAcortada();
    }


    public void eliminarURL(String userId, String urlAcortada)
    {
        GestionDb gestionUsuario = new GestionDb(Usuario.class);


        EntityManager em = gestionUsuario.getEntityManager();
        EntityManager emURL = gestionDb.getEntityManager();

        try
        {
           Usuario user =  em.find(Usuario.class,userId);
           URLs url = em.find(URLs.class,urlAcortada);

           em.getTransaction().begin();
           user.getUrls().remove(url);
           em.merge(user);
           em.getTransaction().commit();

           gestionDb.eliminar(urlAcortada);

        }
        finally {
            em.close();
            emURL.close();
        }
    }

    public void visitar(String urlAcortada, String navegador, String direccionIP, LocalDate fechaAcceso, LocalTime horaAcceso, String sistemaOperativo)
    {

        URLs url = (URLs) gestionDb.find(urlAcortada);
        Cliente cliente = new Cliente(navegador,direccionIP,fechaAcceso, horaAcceso, sistemaOperativo);

        GestionDb gestionDbCliente = new GestionDb(Cliente.class);
        gestionDbCliente.crear(cliente);

        url.getClientes().add(cliente);

        gestionDb.editar(url);

    }

    public List<URLs> getURLs()
    {
        return gestionDb.findAll();
    }


    public Set<URLs> getUrlsCliente() {
        return urlsCliente;
    }

    public void setUrlsCliente(Set<URLs> urlsCliente) {
        this.urlsCliente = urlsCliente;
    }

    public long getCantidadVisitas(String urlAcortada)
    {
        URLs url = (URLs) gestionDb.find(urlAcortada);
        if(url != null)
        {
            return url.getClientes().size();
        }
        else
        {
            return 0;
        }
    }

    public long getCantidadVisitasFecha(String urlAcortada, String fecha)
    {
        URLs url = (URLs) gestionDb.find(urlAcortada);
        long contador = 0;
        for(Cliente cliente : url.getClientes())
        {
            if(cliente.getFechaAcceso().toString().equalsIgnoreCase(fecha))
            {
                contador++;
            }
        }
        return contador;
    }

    public ArrayList<LocalTime> getHorasFecha(String urlAcortada, String fecha)
    {
        URLs url = (URLs) gestionDb.find(urlAcortada);
        ArrayList<LocalTime> horas = new ArrayList<>();
        for(Cliente cliente : url.getClientes())
        {
            if(cliente.getFechaAcceso().toString().equalsIgnoreCase(fecha))
            {
                horas.add(cliente.getHoraAcceso());
            }
        }
        return horas;
    }

    public long getCantidadVisitasNavegador(String urlAcortada, String navegador)
    {
        URLs url = (URLs) gestionDb.find(urlAcortada);
        long contador = 0;
        for(Cliente cliente : url.getClientes())
        {
            if(cliente.getNavegador().toString().equalsIgnoreCase(navegador))
            {
                contador++;
            }
        }
        return contador;
    }

    public URLs getURL(String urlAcortada)
    {
        URLs url = (URLs) gestionDb.find(urlAcortada);
        return url;
    }

    public Set<Cliente> getClientesURLByFecha(String urlAcortada, String fecha)
    {
        URLs url = (URLs) gestionDb.find(urlAcortada);
        Set<Cliente> clientes = new HashSet<>();

        for(Cliente cliente : url.getClientes())
        {
            if(cliente.getFechaAcceso().toString().equals(fecha))
            {
                clientes.add(cliente);
            }
        }
        return clientes;
    }

    public void registrarURLUsuario(String idUser, URLs url)
    {
        GestionDb gestionUsuario = new GestionDb(Usuario.class);
        Usuario user = (Usuario) gestionUsuario.find(idUser);

        EntityManager emUsuario = gestionUsuario.getEntityManager();
        EntityManager emURL = gestionDb.getEntityManager();

        try
        {
            emURL.getTransaction().begin();
                if(gestionDb.find(url.getDireccionAcortada()) == null)
                {
                    emURL.persist(url);
                }
            emURL.getTransaction().commit();

            emUsuario.getTransaction().begin();
                user.getUrls().add(url);
                emUsuario.merge(user);
            emUsuario.getTransaction().commit();
        }finally {
            emURL.close();
            emUsuario.close();
        }
    }

    public Set<Cliente> getClientesByUrl(String idUrl)
    {
        return ((URLs) gestionDb.find(idUrl)).getClientes();
    }

    public Set<Cliente> getClientesByUrlNoUser(String idUrl){
        URLs found = null;
        for (URLs url : this.urlsCliente){
            if(url.getDireccionAcortada().equals(idUrl)){
                found = url;
            }
        }
        return found.getClientes();
    }

    public URLs getUrlNoUser(String idUrl){
        URLs found = null;
        for (URLs url : this.urlsCliente){
            if(url.getDireccionAcortada().equals(idUrl)){
                found = url;
            }
        }
        return found;
    }

}
