package com.ignaziopicciche.albergo.helper;

import com.ignaziopicciche.albergo.dto.PrenotazioneClienteStanzaDTO;
import com.ignaziopicciche.albergo.dto.PrenotazioneDTO;
import com.ignaziopicciche.albergo.exception.HotelException;
import com.ignaziopicciche.albergo.exception.PrenotazioneException;
import com.ignaziopicciche.albergo.model.Cliente;
import com.ignaziopicciche.albergo.model.Prenotazione;
import com.ignaziopicciche.albergo.model.Stanza;
import com.ignaziopicciche.albergo.repository.ClienteRepository;
import com.ignaziopicciche.albergo.repository.HotelRepository;
import com.ignaziopicciche.albergo.repository.PrenotazioneRepository;
import com.ignaziopicciche.albergo.repository.StanzaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PrenotazioneHelper {

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private StanzaRepository stanzaRepository;


    public PrenotazioneDTO findById(Long id) {
        if (prenotazioneRepository.existsById(id)) {
            return new PrenotazioneDTO(prenotazioneRepository.findById(id).get());
        }

        throw new PrenotazioneException(PrenotazioneException.PrenotazioneExceptionCode.PRENOTAZIONE_ID_NOT_EXIST);
    }

    public List<PrenotazioneClienteStanzaDTO> findAll(Long idHotel) {
        if (hotelRepository.existsById(idHotel)) {
            List<Prenotazione> prenotazioni = prenotazioneRepository.findPrenotazionesByHotel_Id(idHotel);
            List<PrenotazioneClienteStanzaDTO> prenotazioniList = new ArrayList<>();

            for (Prenotazione p : prenotazioni) {
                Cliente c = clienteRepository.findById(p.getCliente().getId()).get();
                Stanza s = stanzaRepository.findById(p.getStanza().getId()).get();
                prenotazioniList.add(new PrenotazioneClienteStanzaDTO(p, c, s));
            }

            return prenotazioniList;
        }

        throw new HotelException(HotelException.HotelExceptionCode.HOTEL_ID_NOT_EXIST);
    }


    public Boolean delete(Long id) {
        if (prenotazioneRepository.existsById(id)) {
            try {
                prenotazioneRepository.deleteById(id);
                return true;
            } catch (Exception e) {
                throw new PrenotazioneException(PrenotazioneException.PrenotazioneExceptionCode.PRENOTAZIONE_DELETE_ERROR);
            }
        }

        throw new PrenotazioneException(PrenotazioneException.PrenotazioneExceptionCode.PRENOTAZIONE_NOT_FOUND);
    }


    public PrenotazioneDTO create(PrenotazioneDTO prenotazioneDTO) {

        List<Prenotazione> prenotazioniStanza = prenotazioneRepository.findPrenotazionesByStanza_IdAndHotel_Id(prenotazioneDTO.idStanza, prenotazioneDTO.idHotel);
        Boolean checkDate = true;

        if (!prenotazioneRepository.existsById(prenotazioneDTO.id)) {
            for (Prenotazione ps : prenotazioniStanza) {

                if ((prenotazioneDTO.dataInizio.isAfter(ps.getDataInizio()) && prenotazioneDTO.dataFine.isBefore(ps.getDataFine())) ||
                        (prenotazioneDTO.dataInizio.isBefore(ps.getDataInizio()) && prenotazioneDTO.dataFine.isAfter(ps.getDataFine())) ||
                        (prenotazioneDTO.dataInizio.isBefore(ps.getDataInizio()) && prenotazioneDTO.dataFine.isAfter(ps.getDataInizio())) ||
                        (prenotazioneDTO.dataInizio.isBefore(ps.getDataFine()) && prenotazioneDTO.dataFine.isAfter(ps.getDataFine())) ||
                        (prenotazioneDTO.dataInizio.equals(ps.getDataInizio()) && prenotazioneDTO.dataFine.equals(ps.getDataFine()))) {

                    checkDate = false;
                }

                if (checkDate) {
                    Prenotazione p = new Prenotazione();
                    p.setDataInizio(prenotazioneDTO.dataInizio);
                    p.setDataFine(prenotazioneDTO.dataFine);
                    p.setCliente(clienteRepository.findById(prenotazioneDTO.idCliente).get());
                    p.setStanza(stanzaRepository.findById(prenotazioneDTO.idStanza).get());
                    p.setHotel(hotelRepository.findById(prenotazioneDTO.idHotel).get());

                    prenotazioneRepository.save(p);
                    return new PrenotazioneDTO(p);
                }
            }

            throw new PrenotazioneException(PrenotazioneException.PrenotazioneExceptionCode.PRENOTAZIONE_DATE_NOT_COMPATIBLE);

        }

        throw new PrenotazioneException(PrenotazioneException.PrenotazioneExceptionCode.PRENOTAZIONE_ALREADY_EXISTS);
    }


}
