package gm.zona_fit.repositorio;

import gm.zona_fit.modelo.Pago;
import gm.zona_fit.modelo.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoRepositorio extends JpaRepository<Pago, Integer> {
    List<Pago> findByClienteOrderByFechaDesc(Cliente cliente);
}