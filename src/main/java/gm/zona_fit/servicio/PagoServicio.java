package gm.zona_fit.servicio;

import gm.zona_fit.modelo.Cliente;
import gm.zona_fit.modelo.Pago;
import gm.zona_fit.repositorio.ClienteRepositorio;
import gm.zona_fit.repositorio.PagoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class PagoServicio implements IPagoServicio {

    @Autowired
    private PagoRepositorio pagoRepositorio;

    @Autowired
    private ClienteRepositorio clienteRepositorio;

    @Override
    public void registrarPago(Pago pago) {
        pago.setFecha(LocalDate.now());
        pagoRepositorio.save(pago);

        // Actualizar fecha de vencimiento del cliente
        Cliente cliente = pago.getCliente();
        LocalDate base = (cliente.getFechaVencimiento() != null &&
                cliente.getFechaVencimiento().isAfter(LocalDate.now()))
                ? cliente.getFechaVencimiento()
                : LocalDate.now();
        cliente.setFechaVencimiento(base.plusDays(cliente.getMembresia()));
        clienteRepositorio.save(cliente);
    }

    @Override
    public List<Pago> listarPagosPorCliente(Cliente cliente) {
        return pagoRepositorio.findByClienteOrderByFechaDesc(cliente);
    }

    @Override
    public List<Pago> listarTodos() {
        return pagoRepositorio.findAll();
    }
}