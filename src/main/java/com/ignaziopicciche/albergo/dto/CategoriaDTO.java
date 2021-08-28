package com.ignaziopicciche.albergo.dto;

import com.ignaziopicciche.albergo.enums.CategoriaNome;
import com.ignaziopicciche.albergo.model.Categoria;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaDTO {

    public Long id;
    public CategoriaNome nome;
    public Double prezzo;
    public String descrizione;
    public Long idHotel;

    public CategoriaDTO(Categoria c) {
        this.id = c.getId();
        this.nome = c.getNome();
        this.prezzo = c.getPrezzo();
        this.descrizione = c.getDescrizione();
        this.idHotel = c.getHotel().getId();
    }

}
