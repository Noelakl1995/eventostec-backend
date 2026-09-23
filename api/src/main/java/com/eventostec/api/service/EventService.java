package com.eventostec.api.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.eventostec.api.domain.address.Address;
import com.eventostec.api.domain.coupon.Coupon;
import com.eventostec.api.domain.event.Event;
import com.eventostec.api.domain.event.EventAddressProjection;
import com.eventostec.api.domain.event.EventDetailsDTO;
import com.eventostec.api.domain.event.EventRequestDTO;
import com.eventostec.api.domain.event.EventResponseDTO;
import com.eventostec.api.repositories.EventRepository;

@Service
public class EventService {

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Autowired 
    private EventRepository eventRepository;

    @Autowired
    private AmazonS3 s3client;

    @Autowired
    private AddressService addressService;
    @Autowired 
    private  CouponService couponService;

    public Event createEvent(EventRequestDTO data) {
        // Implementation for creating an event
        String imgUrl = "";
        if (data.image() != null && data.image().length > 0) {
            imgUrl = this.uploadImg(data.image()[0]);
        }
        Event newevent = new Event();
        newevent.setTitle(data.name());
        newevent.setDescription(data.description());
        newevent.setImgUrl(imgUrl);
        newevent.setDate(new Date(data.date()));
        newevent.setImgUrl(imgUrl);
        eventRepository.save(newevent);

        if(!data.remote()){
           this.addressService.createAddress(data, newevent);
        }
        return newevent;


    }

     public List<EventResponseDTO> getUpcomingEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventAddressProjection> eventsPage = this.eventRepository.findUpcomingEvents(new Date(), pageable);
        return eventsPage.map(event -> new EventResponseDTO(
                        event.getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getDate(),
                        event.getCity() != null ? event.getCity() : "",
                        event.getUf() != null ? event.getUf() : "",
                        event.getRemote(),
                        event.getEventUrl(),
                        event.getImgUrl())
                )
                .stream().toList();
    }

    public List<EventResponseDTO> getFilteredEvents(int page, int size, String city, String uf, Date startDate, Date endDate){
        city = (city != null) ? city : "";
        uf = (uf != null) ? uf : "";
        startDate = (startDate != null) ? startDate : new Date(0);
        endDate = (endDate != null) ? endDate : new Date();

        Pageable pageable = PageRequest.of(page, size);

        Page<EventAddressProjection> eventsPage = this.eventRepository.findFilteredEvents(city, uf, startDate, endDate, pageable);
        return eventsPage.map(event -> new EventResponseDTO(
                        event.getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getDate(),
                        event.getCity() != null ? event.getCity() : "",
                        event.getUf() != null ? event.getUf() : "",
                        event.getRemote(),
                        event.getEventUrl(),
                        event.getImgUrl())
                )
                .stream().toList();
    }


     public EventDetailsDTO getEventDetails(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        Optional<Address> address = addressService.findByEventId(eventId);

        List<Coupon> coupons = couponService.consultCoupons(eventId, new Date());

        List<EventDetailsDTO.CouponDTO> couponDTOs = coupons.stream()
                .map(coupon -> new EventDetailsDTO.CouponDTO(
                        coupon.getCode(),
                        coupon.getDiscount(),
                        coupon.getValid()))
                .collect(Collectors.toList());

        return new EventDetailsDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDate(),
                address.isPresent() ? address.get().getCity() : "",
                address.isPresent() ? address.get().getUf() : "",
                event.getImgUrl(),
                event.getEventUrl(),
                couponDTOs);
    }

    
    private String uploadImg(MultipartFile multipartFile) {
        String filename = UUID.randomUUID().toString() + "_" + multipartFile.getOriginalFilename();
        try {
           File file = this.convertMultiPartToFile(multipartFile);
           s3client.putObject(new PutObjectRequest(this.bucketName, filename, file));
           file.delete();
            return s3client.getUrl(this.bucketName, filename).toString();
        } catch (IOException e) {
            System.out.println("Error uploading file: " + e.getMessage());
            return "";
        }
       

    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

}
