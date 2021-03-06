package org.pucmm.web.Controlador;

import io.javalin.Javalin;
import org.pucmm.web.Modelo.Cliente;
import org.pucmm.web.Modelo.URLs;
import org.pucmm.web.Modelo.Usuario;
import org.pucmm.web.Servicio.URLServices;
import org.pucmm.web.Servicio.UsuarioServices;
import org.pucmm.web.util.RolesApp;

import java.time.LocalDate;
import java.util.*;

public class DashboardControlador {

    private Javalin app;
    Map<String, Object> modelo = new HashMap<>();
    Map<String, Object> modeloVistaUsuario = new HashMap<>();
    private String dominio = "https://shorter.jgshopping.games/";
    Set<LocalDate> fechas;
    List<Long> visitasFechas;

    public DashboardControlador(Javalin app)
    {
        this.app = app;
        modelo.put("dominio", dominio);
    }


    public void aplicarRutas() throws NumberFormatException {

        app.config.accessManager((handler, ctx, permittedRoles) -> {
            //para obtener el usuario estaré utilizando el contexto de sesion.
            final Usuario usuario = ctx.sessionAttribute("usuario");

            if(permittedRoles.isEmpty()){
                handler.handle(ctx);
                return;
            }
            //validando si existe el usuario.
            if(usuario == null){
                System.out.println("No hay usuario en sesion..");
                ctx.status(401).result("No tiene permiso para acceder...");
                ctx.redirect("/login");
                return;
            }
            //buscando el permiso del usuario.
            Usuario usuarioTmp = UsuarioServices.getInstancia().getAllUsuarios().stream()
                    .filter(u -> u.getNombreUsuario().equalsIgnoreCase(usuario.getNombreUsuario()))
                    .findAny()
                    .orElse(null);

            if(usuarioTmp==null){
                System.out.println("Existe el usuario pero sin roles para acceder.");
                ctx.status(401).result("No tiene credencial para acceder...");
                return;
            }

            //validando que el usuario registrado tiene el rol permitido.
            RolesApp role = usuarioTmp.getRol();
            if (permittedRoles.contains(role)) {
                handler.handle(ctx);
            }

        });

       app.get("/dashboard", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if(usuario == null) {
                ctx.redirect("/usuario/iniciarSesion");
            }else{
                Usuario user =  UsuarioServices.getInstancia().getUsuario(usuario.getNombreUsuario());

                if(user != null)
                {
                    modelo.put("clientes",null);
                    modelo.put("visitasFechas","");
                    modelo.put("usuarioActual", UsuarioServices.getInstancia().getUsuario(user.getNombreUsuario()));
                    modelo.put("urls", UsuarioServices.getInstancia().getURLsByUsuario(user.getNombreUsuario()));
                    ctx.render("/vistas/templates/dashboard.html",modelo);
                }

            }
        }, Collections.singleton(RolesApp.ROLE_ADMIN));

        app.post("/dashboard/infoOtro", ctx -> {

            if(ctx.sessionAttribute("usuario") == null)
            {
                ctx.redirect("/usuario/iniciarSesion");
            }else
            {
                if(UsuarioServices.getInstancia().getUsuario(ctx.formParam("verUsuario")) == null)
                {
                    ctx.result("El usuario no existe");
                }else
                {
                    modeloVistaUsuario.put("urlActual", ctx.formParam("url"));
                    modeloVistaUsuario.put("clientes",null);
                    modeloVistaUsuario.put("visitasFechas","");
                    modeloVistaUsuario.put("visitasFechas","");
                    modeloVistaUsuario.put("verUsuario", ctx.formParam("verUsuario"));
                    modeloVistaUsuario.put("usuarioActual", UsuarioServices.getInstancia().getUsuario(ctx.sessionAttribute("usuario")));
                    modeloVistaUsuario.put("urls", UsuarioServices.getInstancia().getURLsByUsuario(ctx.formParam("verUsuario")));
                    modeloVistaUsuario.put("dominio", dominio);
                    ctx.render("/vistas/templates/dashboardOtro.html",modeloVistaUsuario);
                }

            }
        });

        app.post("/dashboard/infoURLOtro",ctx -> {

            if(ctx.sessionAttribute("usuario") == null)
            {
                ctx.redirect("/usuario/iniciarSesion");
            }else {
                URLs url = URLServices.getInstance().getURL(ctx.formParam("url"));
                fechas = new HashSet<>();
                visitasFechas = new ArrayList<>();

                for (Cliente cliente : url.getClientes()) {
                    LocalDate date = cliente.getFechaAcceso();
                    fechas.add(date);
                }

                for (LocalDate fecha : fechas) {
                    visitasFechas.add(URLServices.getInstance().getCantidadVisitasFecha(url.getDireccionAcortada(), fecha.toString()));
                }

                modeloVistaUsuario.put("urlActual", ctx.formParam("url"));
                modeloVistaUsuario.put("usuarioActual", UsuarioServices.getInstancia().getUsuario(ctx.sessionAttribute("usuario")));
                modeloVistaUsuario.put("fechaAcceso", "");
                modeloVistaUsuario.put("fechas", fechas);
                modeloVistaUsuario.put("visitasFechas", visitasFechas);
                modeloVistaUsuario.put("clientes", new HashSet<Cliente>());
                ctx.render("/vistas/templates/dashboardOtro.html", modeloVistaUsuario);
            }
        });

        app.get("/dashboard/infoURL", ctx -> {
            if(ctx.sessionAttribute("usuario") == null)
            {
                ctx.redirect("/usuario/iniciarSesion");
            }else {
                ctx.render("/vistas/templates/infoUrl.html", modelo);
            }
        }, Collections.singleton(RolesApp.ROLE_ADMIN));

        app.get("/dashboard/infoURLOtro", ctx -> {
            if(ctx.sessionAttribute("usuario") == null)
            {
                ctx.redirect("/usuario/iniciarSesion");
            }else {
                ctx.render("/vistas/templates/dashboardOtro.html", modeloVistaUsuario);
            }
        });

        app.post("/dashboard/infoURL", ctx -> {
            if(ctx.sessionAttribute("usuario") == null)
            {
                ctx.redirect("/usuario/iniciarSesion");
            }else {
                URLs url = URLServices.getInstance().getURL(ctx.formParam("url"));
                fechas = new HashSet<>();
                visitasFechas = new ArrayList<>();

                for (Cliente cliente : url.getClientes()) {
                    LocalDate date = cliente.getFechaAcceso();
                    fechas.add(date);
                }

                for (LocalDate fecha : fechas) {
                    visitasFechas.add(URLServices.getInstance().getCantidadVisitasFecha(url.getDireccionAcortada(), fecha.toString()));
                }

                modelo.put("urlActual", ctx.formParam("url"));
                modelo.put("fechaAcceso", "");
                modelo.put("clientes", URLServices.getInstance().getClientesByUrl(ctx.formParam("url")));
                modelo.put("fechas", fechas);
                modelo.put("visitasFechas", visitasFechas);
                ctx.render("/vistas/templates/infoUrl.html", modelo);
            }
        }, Collections.singleton(RolesApp.ROLE_ADMIN));

        app.get("/dashboard/infoURL/:url/estadisticas/:fecha",ctx->{
            if(ctx.sessionAttribute("usuario") == null)
            {
                ctx.redirect("/usuario/iniciarSesion");
            }else {
                URLs url = URLServices.getInstance().getURL(ctx.pathParam("url"));
                fechas = new HashSet<>();
                visitasFechas = new ArrayList<>();

                for (Cliente cliente : url.getClientes()) {
                    LocalDate date = cliente.getFechaAcceso();
                    fechas.add(date);
                }

                for (LocalDate fecha : fechas) {
                    visitasFechas.add(URLServices.getInstance().getCantidadVisitasFecha(url.getDireccionAcortada(), fecha.toString()));
                }


                modelo.put("fechaAcceso", ctx.pathParam("fecha"));
                modelo.put("fechas", fechas);
                modelo.put("visitasFechas", visitasFechas);
                modelo.put("clientes", URLServices.getInstance().getClientesURLByFecha(
                        ctx.pathParam("url"), ctx.pathParam("fecha")
                ));
                ctx.redirect("/dashboard/infoURL");
            }
        }, Collections.singleton(RolesApp.ROLE_ADMIN));

        app.get("/dashboard/infoURLOtro/:url/estadisticas/:fecha",ctx->{
            if(ctx.sessionAttribute("usuario") == null)
            {
                ctx.redirect("/usuario/iniciarSesion");
            }else {
                URLs url = URLServices.getInstance().getURL(ctx.pathParam("url"));
                fechas = new HashSet<>();
                visitasFechas = new ArrayList<>();

                for (Cliente cliente : url.getClientes()) {
                    LocalDate date = cliente.getFechaAcceso();
                    fechas.add(date);
                }

                for (LocalDate fecha : fechas) {
                    visitasFechas.add(URLServices.getInstance().getCantidadVisitasFecha(url.getDireccionAcortada(), fecha.toString()));
                }

                modeloVistaUsuario.put("fechaAcceso", ctx.pathParam("fecha"));
                modeloVistaUsuario.put("fechas", fechas);
                modeloVistaUsuario.put("visitasFechas", visitasFechas);
                modeloVistaUsuario.put("clientes", URLServices.getInstance().getClientesURLByFecha(
                        ctx.pathParam("url"), ctx.pathParam("fecha")
                ));
                ctx.redirect("/dashboard/infoURLOtro");
            }
        }, Collections.singleton(RolesApp.ROLE_ADMIN));

        //Eliminacion de URLs de otros usuarios
        app.post("/url/eliminar-otro", ctx -> {
            URLServices.getInstance().eliminarURL(modeloVistaUsuario.get("verUsuario").toString(), ctx.formParam("eliminar"));
            modeloVistaUsuario.put("urls", UsuarioServices.getInstancia().getURLsByUsuario(modeloVistaUsuario.get("verUsuario").toString()));
            ctx.redirect("/dashboard/infoURLOtro");
        }, Collections.singleton(RolesApp.ROLE_ADMIN));

        app.post("/misLinks/infoUrl", ctx -> {

            URLs url = URLServices.getInstance().getURL(ctx.formParam("url"));
            fechas = new HashSet<>();
            visitasFechas = new ArrayList<>();

            for (Cliente cliente : url.getClientes()) {
                LocalDate date = cliente.getFechaAcceso();
                fechas.add(date);
            }

            for (LocalDate fecha : fechas) {
                visitasFechas.add(URLServices.getInstance().getCantidadVisitasFecha(url.getDireccionAcortada(), fecha.toString()));
            }

            modelo.put("urlActual", ctx.formParam("url"));
            modelo.put("fechaAcceso", "");
            modelo.put("clientes", URLServices.getInstance().getClientesByUrl(ctx.formParam("url")));
            modelo.put("fechas", fechas);
            modelo.put("visitasFechas", visitasFechas);
            ctx.render("/vistas/templates/infoUrl.html", modelo);
        });

    }
}
