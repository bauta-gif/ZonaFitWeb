package gm.zona_fit.bean;

import gm.zona_fit.modelo.Cliente;
import gm.zona_fit.modelo.Pago;
import gm.zona_fit.servicio.IClienteServicio;
import gm.zona_fit.servicio.IPagoServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.event.SelectEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Named("clienteBean")
@ViewScoped
@Getter
@Setter
public class ClienteBean implements Serializable {

    private List<Cliente> clientes;
    private List<Cliente> clientesFiltrados;
    private Cliente clienteSeleccionado;
    private String filtro = "";

    // Para pagos
    private List<Pago> pagosPorCliente;
    private Pago nuevoPago = new Pago();
    private String[] metodosPago = {"EFECTIVO", "TARJETA", "TRANSFERENCIA"};

    private IClienteServicio getServicio() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ServletContext sc = (ServletContext) fc.getExternalContext().getContext();
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sc);
        return ctx.getBean(IClienteServicio.class);
    }

    private IPagoServicio getPagoServicio() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ServletContext sc = (ServletContext) fc.getExternalContext().getContext();
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sc);
        return ctx.getBean(IPagoServicio.class);
    }

    @PostConstruct
    public void init() {
        clienteSeleccionado = new Cliente();
        nuevoPago = new Pago();
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
        pagosPorCliente = getPagoServicio().listarPagosPorCliente(clienteSeleccionado);
        nuevoPago = new Pago();
        nuevoPago.setCliente(clienteSeleccionado);
    }

    public void guardarCliente() {
        boolean esNuevo = (clienteSeleccionado.getId() == null);
        getServicio().guardarCliente(clienteSeleccionado);
        String msg = esNuevo ? "Cliente agregado" : "Cliente actualizado";
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

    public void registrarPago() {
        if (nuevoPago.getMonto() == null || nuevoPago.getMonto() <= 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ingresá un monto válido", null));
            return;
        }
        nuevoPago.setCliente(clienteSeleccionado);
        getPagoServicio().registrarPago(nuevoPago);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Pago registrado", null));
        pagosPorCliente = getPagoServicio().listarPagosPorCliente(clienteSeleccionado);
        // Refrescar el cliente para ver la nueva fecha de vencimiento
        clienteSeleccionado = getServicio().buscarClientePorId(clienteSeleccionado.getId());
        nuevoPago = new Pago();
        nuevoPago.setCliente(clienteSeleccionado);
        cargarClientes();
    }

    // Días restantes de membresía
    public long getDiasRestantes(Cliente c) {
        if (c.getFechaVencimiento() == null) return 0;
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), c.getFechaVencimiento());
        return Math.max(dias, 0);
    }

    // Estado de la membresía para mostrar alerta
    public String getEstadoMembresia(Cliente c) {
        long dias = getDiasRestantes(c);
        if (dias == 0) return "VENCIDA";
        if (dias <= 5) return "POR VENCER";
        return "ACTIVA";
    }

    public void limpiarFormulario() {
        clienteSeleccionado = new Cliente();
        pagosPorCliente = null;
        nuevoPago = new Pago();
        filtro = "";
        clientesFiltrados = clientes;
    }

    public Cliente getClienteSeleccionado() {
        if (clienteSeleccionado == null) clienteSeleccionado = new Cliente();
        return clienteSeleccionado;
    }

    // Exportar reporte a Excel
    public void exportarExcel() throws IOException {
        List<Pago> pagos = getPagoServicio().listarTodos();

        Workbook workbook = new XSSFWorkbook();

        // Hoja 1 - Clientes
        Sheet hojaClientes = workbook.createSheet("Clientes");
        Row cabClientes = hojaClientes.createRow(0);
        cabClientes.createCell(0).setCellValue("ID");
        cabClientes.createCell(1).setCellValue("Nombre");
        cabClientes.createCell(2).setCellValue("Apellido");
        cabClientes.createCell(3).setCellValue("Membresía (días)");
        cabClientes.createCell(4).setCellValue("Vencimiento");
        cabClientes.createCell(5).setCellValue("Estado");

        int fila = 1;
        for (Cliente c : clientes) {
            Row r = hojaClientes.createRow(fila++);
            r.createCell(0).setCellValue(c.getId());
            r.createCell(1).setCellValue(c.getNombre());
            r.createCell(2).setCellValue(c.getApellido() != null ? c.getApellido() : "");
            r.createCell(3).setCellValue(c.getMembresia());
            r.createCell(4).setCellValue(c.getFechaVencimiento() != null
                    ? c.getFechaVencimiento().toString() : "Sin pagos");
            r.createCell(5).setCellValue(getEstadoMembresia(c));
        }

        // Hoja 2 - Pagos
        Sheet hojaPagos = workbook.createSheet("Historial de pagos");
        Row cabPagos = hojaPagos.createRow(0);
        cabPagos.createCell(0).setCellValue("ID Pago");
        cabPagos.createCell(1).setCellValue("Cliente");
        cabPagos.createCell(2).setCellValue("Fecha");
        cabPagos.createCell(3).setCellValue("Monto");
        cabPagos.createCell(4).setCellValue("Método");
        cabPagos.createCell(5).setCellValue("Observaciones");

        fila = 1;
        double totalIngresos = 0;
        for (Pago p : pagos) {
            Row r = hojaPagos.createRow(fila++);
            r.createCell(0).setCellValue(p.getId());
            r.createCell(1).setCellValue(p.getCliente().getNombre() + " " +
                    (p.getCliente().getApellido() != null ? p.getCliente().getApellido() : ""));
            r.createCell(2).setCellValue(p.getFecha().toString());
            r.createCell(3).setCellValue(p.getMonto());
            r.createCell(4).setCellValue(p.getMetodoPago());
            r.createCell(5).setCellValue(p.getObservaciones() != null ? p.getObservaciones() : "");
            totalIngresos += p.getMonto();
        }
        // Total al final
        Row total = hojaPagos.createRow(fila + 1);
        total.createCell(2).setCellValue("TOTAL INGRESOS:");
        total.createCell(3).setCellValue(totalIngresos);

        // Hoja 3 - Estadísticas
        Sheet hojaStats = workbook.createSheet("Estadísticas");
        hojaStats.createRow(0).createCell(0).setCellValue("Total clientes");
        hojaStats.getRow(0).createCell(1).setCellValue(clientes.size());
        hojaStats.createRow(1).createCell(0).setCellValue("Total ingresos");
        hojaStats.getRow(1).createCell(1).setCellValue(totalIngresos);
        hojaStats.createRow(2).createCell(0).setCellValue("Membresías vencidas");
        hojaStats.getRow(2).createCell(1).setCellValue(
                clientes.stream().filter(c -> getEstadoMembresia(c).equals("VENCIDA")).count());
        hojaStats.createRow(3).createCell(0).setCellValue("Por vencer (≤5 días)");
        hojaStats.getRow(3).createCell(1).setCellValue(
                clientes.stream().filter(c -> getEstadoMembresia(c).equals("POR VENCER")).count());

        // Enviar al browser
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_zonafit.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
        fc.responseComplete();
    }
}