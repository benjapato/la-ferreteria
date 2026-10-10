package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Categoria;
import com.LaFerreteria.LaFerreteria.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    public Optional<Categoria> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    public Categoria guardar(Categoria categoria) {

        if (categoriaRepository.existsByNombreIgnoreCase(categoria.getNombre())) {
            throw new IllegalArgumentException(
                    "Ya existe una categoría con el nombre " + categoria.getNombre()
            );
        }

        return categoriaRepository.save(categoria);
    }

    public Categoria actualizar(Long id, Categoria datosCategoria) {

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("No existe la categoría con ID " + id)
                );

        Optional<Categoria> categoriaConMismoNombre =
                categoriaRepository.findByNombreIgnoreCase(datosCategoria.getNombre());

        if (categoriaConMismoNombre.isPresent()
                && !categoriaConMismoNombre.get().getId().equals(id)) {

            throw new IllegalArgumentException(
                    "Ya existe otra categoría con ese nombre"
            );
        }

        categoria.setNombre(datosCategoria.getNombre());
        categoria.setDescripcion(datosCategoria.getDescripcion());

        return categoriaRepository.save(categoria);
    }
    public void cambiarEstado(Long id) {

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la categoría con ID " + id
                        )
                );

        categoria.setActiva(
                !Boolean.TRUE.equals(categoria.getActiva())
        );

        categoriaRepository.save(categoria);
    }
    public List<Categoria> listarActivas() {
        return categoriaRepository.findByActivaTrueOrderByNombreAsc();
    }
}