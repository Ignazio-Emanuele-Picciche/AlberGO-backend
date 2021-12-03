package com.ignaziopicciche.albergo.helper;

import com.ignaziopicciche.albergo.dto.ClienteDTO;
import com.ignaziopicciche.albergo.enums.ClienteEnum;
import com.ignaziopicciche.albergo.enums.HotelEnum;
import com.ignaziopicciche.albergo.enums.Ruolo;
import com.ignaziopicciche.albergo.handler.ApiRequestException;
import com.ignaziopicciche.albergo.model.Cliente;
import com.ignaziopicciche.albergo.model.ClienteHotel;
import com.ignaziopicciche.albergo.model.Hotel;
import com.ignaziopicciche.albergo.repository.ClienteHotelRepository;
import com.ignaziopicciche.albergo.repository.ClienteRepository;
import com.ignaziopicciche.albergo.repository.HotelRepository;
import com.ignaziopicciche.albergo.security.models.Amministratore;
import com.ignaziopicciche.albergo.security.models.AuthenticationRequest;
import com.ignaziopicciche.albergo.security.models.AuthenticationResponse;
import com.ignaziopicciche.albergo.security.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ClienteHelper implements UserDetailsService {

    private final ClienteRepository clienteRepository;
    private final HotelRepository hotelRepository;
    private final ClienteHotelRepository clienteHotelRepository;
    private final StripeHelper stripeHelper;
    private final ClienteHotelHelper clienteHotelHelper;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtTokenUtil;

    private static ClienteEnum clienteEnum;
    private static HotelEnum hotelEnum;

    public ClienteHelper(ClienteRepository clienteRepository, HotelRepository hotelRepository, StripeHelper stripeHelper, ClienteHotelRepository clienteHotelRepository, ClienteHotelHelper clienteHotelHelper, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtUtil jwtTokenUtil) {
        this.clienteRepository = clienteRepository;
        this.hotelRepository = hotelRepository;
        this.stripeHelper = stripeHelper;
        this.clienteHotelRepository = clienteHotelRepository;
        this.clienteHotelHelper = clienteHotelHelper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public Long create(ClienteDTO clienteDTO) throws Exception {

        if (!clienteRepository.existsByDocumentoOrUsername(clienteDTO.documento, clienteDTO.username) &&
                !clienteDTO.documento.equals("") && !clienteDTO.username.equals("")) {

            Cliente cliente = Cliente.builder()
                    .nome(clienteDTO.nome)
                    .cognome(clienteDTO.cognome)
                    .documento(clienteDTO.documento)
                    .telefono(clienteDTO.telefono)
                    .username(clienteDTO.username)
                    .password(passwordEncoder.encode(clienteDTO.password)).build();

            cliente = clienteRepository.save(cliente);

            List<Hotel> hotels = hotelRepository.findAll();

            if (!hotels.isEmpty()) {
                for (Hotel hotel : hotels) {
                    String customerId = stripeHelper.createCustomer(cliente, hotel.getPublicKey());
                    clienteHotelHelper.createByCliente(cliente, customerId, hotel);

                    stripeHelper.addClienteHotelCarta(cliente);
                }
            }

            return cliente.getId();
        }

        clienteEnum = ClienteEnum.getClienteEnumByMessageCode("CLI_AE");
        throw new ApiRequestException(clienteEnum.getMessage());
    }


    public ClienteDTO update(ClienteDTO clienteDTO) {

        if (clienteRepository.existsById(clienteDTO.id)) {

            Cliente cliente = clienteRepository.findById(clienteDTO.id).get();

            cliente.setNome(clienteDTO.nome);
            cliente.setCognome(clienteDTO.cognome);
            cliente.setTelefono(clienteDTO.telefono);

            clienteRepository.save(cliente);
            return new ClienteDTO(cliente);
        }

        clienteEnum = ClienteEnum.getClienteEnumByMessageCode("CLI_NF");
        throw new ApiRequestException(clienteEnum.getMessage());
    }


    @Transactional
    public Boolean delete(Long idCliente) {

        if (clienteRepository.existsById(idCliente)) {
            try {
                List<ClienteHotel> clientiHotel = clienteHotelRepository.findByCliente_Id(idCliente);
                stripeHelper.deleteCustomerById(clientiHotel);
                clienteHotelRepository.deleteAllByCliente_Id(idCliente);

                clienteRepository.deleteById(idCliente);
                return true;
            } catch (Exception e) {
                clienteEnum = ClienteEnum.getClienteEnumByMessageCode("CLI_DLE");
                throw new ApiRequestException(clienteEnum.getMessage());
            }
        }

        clienteEnum = ClienteEnum.getClienteEnumByMessageCode("CLI_IDNE");
        throw new ApiRequestException(clienteEnum.getMessage());
    }


    public ClienteDTO findById(Long id) {
        if (clienteRepository.existsById(id)) {
            return new ClienteDTO(clienteRepository.findById(id).get());
        }

        clienteEnum = ClienteEnum.getClienteEnumByMessageCode("CLI_IDNE");
        throw new ApiRequestException(clienteEnum.getMessage());
    }


    public List<ClienteDTO> findAll(Long idHotel) {
        if (hotelRepository.existsById(idHotel)) {
            return clienteRepository.findClientiByHotel_Id(idHotel).stream().map(ClienteDTO::new).collect(Collectors.toList());
        }

        hotelEnum = HotelEnum.getHotelEnumByMessageCode("HOT_IDNE");
        throw new ApiRequestException(hotelEnum.getMessage());
    }

    public List<ClienteDTO> findAllByNomeCognome(String nome, String cognome, Long idHotel) {
        List<Cliente> clienti;

        if (cognome == null && nome != null) {
            clienti = clienteRepository.findClientesByNomeStartingWith(nome, idHotel);
            return clienti.stream().map(ClienteDTO::new).collect(Collectors.toList());
        } else if (nome == null && cognome != null) {
            clienti = clienteRepository.findClientesByCognomeStartingWith(cognome, idHotel);
            return clienti.stream().map(ClienteDTO::new).collect(Collectors.toList());
        }

        clienteEnum = ClienteEnum.getClienteEnumByMessageCode("CLI_NF");
        throw new ApiRequestException(clienteEnum.getMessage());
    }


    public ResponseEntity<?> createAuthenticationToken(AuthenticationRequest authenticationRequest) throws Exception {
        try { //gestisco l'eccezione in caso l'autenticazione fallisce
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Incorrect username or password", e);
        }

        final UserDetails userDetails = loadUserByUsername(authenticationRequest.getUsername());
        final String jwt = jwtTokenUtil.generateToken(userDetails, Ruolo.ROLE_CLIENT);  //prendo il token
        return ResponseEntity.ok(new AuthenticationResponse(jwt));  //mi resituisce il token associato all'utente
    }


    //TODO Da gestire l'unicità dello username
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Cliente user = clienteRepository.findClienteByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), new ArrayList<>());  //ho creato un semplice utente con username e password "foo"
    }

}
