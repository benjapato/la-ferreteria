package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Cliente;
import com.LaFerreteria.LaFerreteria.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(
            ClienteRepository clienteRepository
    ) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarActivos() {
        return clienteRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    @Transactional
    public Cliente guardar(Cliente cliente) {

        validarCliente(cliente);

        String documentoNormalizado =
                normalizarTexto(cliente.getDocumento());

        validarDocumentoRepetido(
                documentoNormalizado,
                cliente.getId()
        );

        if (cliente.getId() == null) {

            cliente.setSaldoCuentaCorriente(
                    BigDecimal.ZERO
            );

            cliente.setActivo(true);

        } else {

            Cliente clienteExistente =
                    clienteRepository.findById(cliente.getId())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "El cliente que intenta editar no existe"
                                    )
                            );

            /*
             * No permitimos modificar el saldo desde
             * el formulario de clientes.
             */
            cliente.setSaldoCuentaCorriente(
                    clienteExistente.getSaldoCuentaCorriente()
            );

            cliente.setActivo(
                    clienteExistente.getActivo()
            );
        }

        cliente.setNombre(
                cliente.getNombre().trim()
        );

        cliente.setDocumento(documentoNormalizado);

        cliente.setTelefono(
                normalizarTexto(cliente.getTelefono())
        );

        cliente.setEmail(
                normalizarTexto(cliente.getEmail())
        );

        cliente.setDireccion(
                normalizarTexto(cliente.getDireccion())
        );

        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente cambiarEstado(Long id) {

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el cliente con ID " + id
                        )
                );

        cliente.setActivo(
                !Boolean.TRUE.equals(cliente.getActivo())
        );

        return clienteRepository.save(cliente);
    }

    private void validarCliente(Cliente cliente) {

        if (cliente == null) {
            throw new IllegalArgumentException(
                    "El cliente no puede ser nulo"
            );
        }

        if (cliente.getNombre() == null
                || cliente.getNombre().isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre del cliente es obligatorio"
            );
        }
    }

    private void validarDocumentoRepetido(
            String documento,
            Long clienteId
    ) {

        if (documento == null) {
            return;
        }

        clienteRepository.findByDocumento(documento)
                .ifPresent(clienteEncontrado -> {

                    if (!Objects.equals(
                            clienteEncontrado.getId(),
                            clienteId
                    )) {
                        throw new IllegalArgumentException(
                                "Ya existe un cliente con ese documento"
                        );
                    }
                });
    }

    private String normalizarTexto(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscar(String texto) {

        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }

        return clienteRepository
                .buscarPorNombreODocumento(
                        texto.trim()
                );
    }

    @Transactional(readOnly = true)
    public Page<Cliente> buscar(
            String texto,
            Pageable pageable
    ) {

        if (texto == null || texto.isBlank()) {
            return clienteRepository.findAll(pageable);
        }

        return clienteRepository.buscarPaginaPorNombreODocumento(
                texto.trim(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Cliente> buscarCatalogo(
            String texto,
            Boolean activo,
            boolean soloDeuda,
            boolean soloAlDia,
            boolean soloAFavor,
            Pageable pageable
    ) {
        String textoNormalizado = texto == null || texto.isBlank()
                ? ""
                : texto.trim();

        return clienteRepository.buscarCatalogo(
                textoNormalizado,
                activo,
                soloDeuda,
                soloAlDia,
                soloAFavor,
                pageable
        );
    }
}
