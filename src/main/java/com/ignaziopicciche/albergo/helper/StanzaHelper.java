package com.ignaziopicciche.albergo.helper;

import com.ignaziopicciche.albergo.dto.CategoriaDTO;
import com.ignaziopicciche.albergo.dto.StanzaDTO;
import com.ignaziopicciche.albergo.exception.HotelException;
import com.ignaziopicciche.albergo.exception.StanzaException;
import com.ignaziopicciche.albergo.model.Stanza;
import com.ignaziopicciche.albergo.repository.CategoriaRepository;
import com.ignaziopicciche.albergo.repository.HotelRepository;
import com.ignaziopicciche.albergo.repository.StanzaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StanzaHelper {

    @Autowired
    private StanzaRepository stanzaRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;


    public StanzaDTO create(StanzaDTO stanzaDTO) {

        if (!stanzaRepository.existsStanzaByNumeroStanzaAndHotel_Id(stanzaDTO.numeroStanza, stanzaDTO.idHotel) &&
                stanzaDTO.numeroStanza != null && !stanzaDTO.descrizione.equals("")) {

            Stanza stanza = new Stanza();

            stanza.setNumeroStanza(stanzaDTO.numeroStanza);
            stanza.setFuoriServizio(stanzaDTO.fuoriServizio);
            stanza.setDescrizione(stanzaDTO.descrizione);
            stanza.setMetriQuadri(stanzaDTO.metriQuadri);
            stanza.setHotel(hotelRepository.findById(stanzaDTO.idHotel).get());
            stanza.setCategoria(categoriaRepository.findById(stanzaDTO.idCategoria).get());

            stanzaRepository.save(stanza);
            return new StanzaDTO(stanza);
        }

        throw new StanzaException(StanzaException.StanzaExceptionCode.STANZA_ALREADY_EXISTS);
    }

    public Boolean delete(Long id) {
        if (stanzaRepository.existsById(id)) {
            try {
                stanzaRepository.deleteById(id);
                return true;
            } catch (Exception e) {
                throw new StanzaException(StanzaException.StanzaExceptionCode.STANZA_DELETE_ERROR);
            }
        }

        throw new StanzaException(StanzaException.StanzaExceptionCode.STANZA_ID_NOT_EXIST);
    }


    public StanzaDTO update(StanzaDTO stanzaDTO) {

        if (stanzaRepository.existsById(stanzaDTO.id)) {

            Stanza stanza = stanzaRepository.findById(stanzaDTO.id).get();

            //stanza.setNumeroStanza(stanzaDTO.numeroStanza);
            stanza.setDescrizione(stanzaDTO.descrizione);
            stanza.setFuoriServizio(stanzaDTO.fuoriServizio);
            //stanza.setMetriQuadri(stanzaDTO.metriQuadri);
            //stanza.setHotel(hotelRepository.findById(stanzaDTO.idHotel).get());
            //stanza.setCategoria(categoriaRepository.findById(stanzaDTO.idCategoria).get());

            stanzaRepository.save(stanza);
            return new StanzaDTO(stanza);
        }

        throw new StanzaException(StanzaException.StanzaExceptionCode.STANZA_ID_NOT_EXIST);
    }


    public StanzaDTO findById(Long id) {
        if (stanzaRepository.existsById(id)) {
            return new StanzaDTO(stanzaRepository.findById(id).get());
        }
        throw new StanzaException(StanzaException.StanzaExceptionCode.STANZA_NOT_FOUND);
    }


    public List<StanzaDTO> findAll(Long idHotel) {
        if (hotelRepository.existsById(idHotel)) {
            return stanzaRepository.findStanzasByHotel_Id(idHotel).stream().map(x -> new StanzaDTO(x)).collect(Collectors.toList());
        }

        throw new HotelException(HotelException.HotelExceptionCode.HOTEL_ID_NOT_EXIST);
    }

}
