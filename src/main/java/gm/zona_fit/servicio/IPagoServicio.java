package gm.zona_fit.servicio;

import gm.zona_fit.modelo.Cliente;
import gm.zona_fit.modelo.Pago;
import java.util.List;

public interface IPagoServicio {
    void registrarPago(Pago pago);
    List<Pago> listarPagosPorCliente(Cliente cliente);
    List<Pago> listarTodos();
}