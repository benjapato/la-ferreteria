package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Proveedor;
import com.LaFerreteria.LaFerreteria.repository.ProveedorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public List<Proveedor> listarTodos() {
        return proveedorRepository.findAllByOrderByNombreAsc();
    }

    public List<Proveedor> listarActivos() {
        return proveedorRepository.findByActivoTrueOrderByNombreAsc();
    }

    public Optional<Proveedor> buscarPorId(Long id) {
        return proveedorRepository.findById(id);
    }

    public Proveedor guardar(Proveedor proveedor) {

        normalizarDatos(proveedor);

        if (proveedor.getCuit() != null
                && proveedorRepository.existsByCuit(proveedor.getCuit())) {

            throw new IllegalArgumentException(
                    "Ya existe un proveedor con el CUIT "
                            + proveedor.getCuit()
            );
        }

        proveedor.setActivo(true);

        return proveedorRepository.save(proveedor);
    }

    public Proveedor actualizar(
            Long id,
            Proveedor datosProveedor
    ) {

        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el proveedor con ID " + id
                        )
                );

        normalizarDatos(datosProveedor);

        if (datosProveedor.getCuit() != null) {

            Optional<Proveedor> proveedorConMismoCuit =
                    proveedorRepository.findByCuit(
                            datosProveedor.getCuit()
                    );

            if (proveedorConMismoCuit.isPresent()
                    && !proveedorConMismoCuit
                    .get()
                    .getId()
                    .equals(id)) {

                throw new IllegalArgumentException(
                        "Ya existe otro proveedor con ese CUIT"
                );
            }
        }

        proveedor.setNombre(datosProveedor.getNombre());
        proveedor.setRazonSocial(datosProveedor.getRazonSocial());
        proveedor.setCuit(datosProveedor.getCuit());
        proveedor.setTelefono(datosProveedor.getTelefono());
        proveedor.setEmail(datosProveedor.getEmail());
        proveedor.setDireccion(datosProveedor.getDireccion());

        return proveedorRepository.save(proveedor);
    }

    public void cambiarEstado(Long id) {

        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el proveedor con ID " + id
                        )
                );

        proveedor.setActivo(
                !Boolean.TRUE.equals(proveedor.getActivo())
        );

        proveedorRepository.save(proveedor);
    }

    private void normalizarDatos(Proveedor proveedor) {

        if (proveedor.getNombre() != null) {
            proveedor.setNombre(
                    proveedor.getNombre().trim()
            );
        }

        proveedor.setRazonSocial(
                normalizarTextoOpcional(proveedor.getRazonSocial())
        );

        proveedor.setTelefono(
                normalizarTextoOpcional(proveedor.getTelefono())
        );

        proveedor.setDireccion(
                normalizarTextoOpcional(proveedor.getDireccion())
        );

        proveedor.setEmail(
                normalizarTextoOpcional(proveedor.getEmail())
        );

        if (proveedor.getEmail() != null) {
            proveedor.setEmail(
                    proveedor.getEmail().toLowerCase()
            );
        }

        proveedor.setCuit(
                normalizarCuit(proveedor.getCuit())
        );
    }

    private String normalizarTextoOpcional(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }

    private String normalizarCuit(String cuit) {

        if (cuit == null || cuit.isBlank()) {
            return null;
        }

        String numeros = cuit.replaceAll("\\D", "");

        if (numeros.length() != 11) {
            throw new IllegalArgumentException(
                    "El CUIT debe tener 11 números"
            );
        }

        return numeros.substring(0, 2)
                + "-"
                + numeros.substring(2, 10)
                + "-"
                + numeros.substring(10);
    }
}