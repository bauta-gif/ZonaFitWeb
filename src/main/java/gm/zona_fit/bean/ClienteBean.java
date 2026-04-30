package gm.zona_fit.bean;

import gm.zona_fit.modelo.Cliente;
import gm.zona_fit.servicio.IClienteServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.SelectEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.ServletContext;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Named("clienteBean")
@jakarta.faces.view.ViewScoped
@Getter
@Setter
public class ClienteBean implements Serializable {

    private List<Cliente> clientes;
    private List<Cliente> clientesFiltrados;
    private Cliente clienteSeleccionado = new Cliente();
    private String filtro = "";

    // Obtenemos el servicio de Spring manualmente, sin @Inject ni @Autowired
    private IClienteServicio getServicio() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ServletContext servletContext = (ServletContext) facesContext
                .getExternalContext().getContext();
        ApplicationContext springContext = WebApplicationContextUtils
                .getWebApplicationContext(servletContext);
        return springContext.getBean(IClienteServicio.class);
    }

    @PostConstruct
    public void init() {
        cargarClientes();
    }

    private void cargarClientes() {
        clientes = getServicio().listarCliente();
        filtrar();
    }

    public void filtrar() {
        if (clientes == null) return;
        if (filtro == null || filtro.isBlank()) {
            clientesFiltrados = clientes;
        } else {
            String f = filtro.toLowerCase();
            clientesFiltrados = clientes.stream()
                    .filter(c -> c.getNombre().toLowerCase().contains(f)
                            || (c.getApellido() != null
                            && c.getApellido().toLowerCase().contains(f)))
                    .collect(Collectors.toList());
        }
    }

    public void onRowSelect(SelectEvent<Cliente> event) {
        clienteSeleccionado = event.getObject();
    }

    public void guardarCliente() {
        System.out.println("ENTRANDO A GUARDAR");
        boolean esNuevo = (clienteSeleccionado.getId() == null);
        getServicio().guardarCliente(clienteSeleccionado);
        String msg = esNuevo
                ? "Cliente agregado correctamente"
                : "Cliente actualizado correctamente";
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
        limpiarFormulario();
        cargarClientes();
    }

    public void eliminarCliente() {
        getServicio().eliminarCliente(clienteSeleccionado);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Cliente eliminado", null));
        limpiarFormulario();
        cargarClientes();
    }

    public void limpiarFormulario() {
        clienteSeleccionado = new Cliente();
        filtro = "";
        clientesFiltrados = clientes;
    }
}