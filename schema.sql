--
-- PostgreSQL database dump
--

\restrict 2cyb3c7zHRYAmNicIyHH9r2FgcvwerUblqUhORQS4zg0fYPfBVcBGn6RDLTuSCw

-- Dumped from database version 15.16
-- Dumped by pg_dump version 15.16

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: emergency_requests; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.emergency_requests (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(2000) NOT NULL,
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id uuid NOT NULL,
    latitude double precision,
    longitude double precision,
    priority character varying(255),
    assigned_responder_id uuid,
    CONSTRAINT emergency_requests_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'EMERGENCY'::character varying, 'RESOLVED'::character varying])::text[]))),
    CONSTRAINT emergency_requests_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'RESOLVED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT emergency_requests_type_check CHECK (((type)::text = ANY ((ARRAY['SAFE'::character varying, 'MEDICAL'::character varying, 'RESCUE_DEBRIS'::character varying, 'FIRE'::character varying, 'SUPPLY_WATER'::character varying, 'SHELTER'::character varying, 'DANGER'::character varying, 'POWER_OUTAGE'::character varying, 'OTHER'::character varying, 'FOOD'::character varying, 'RESCUE'::character varying, 'ACCIDENT'::character varying, 'POLICE'::character varying])::text[])))
);


ALTER TABLE public.emergency_requests OWNER TO postgres;

-- ... (rest of the tables)

--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.users (id, username, phone_number, password, first_name, last_name, city, district, created_at)
VALUES ('00000000-0000-0000-0000-000000000000', 'mesh_anonim', '0000000000', '$2a$10$8.UnVuG9shgIZ3JJ1NDwSOTnHWwDqF7rj.O8H.WvS2t.j.xYwYwY2', 'Anonim', 'Kullanıcı', 'MESH', 'MESH', CURRENT_TIMESTAMP)
ON CONFLICT (phone_number) DO NOTHING;

--
-- Name: locations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.locations (
    id uuid NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    "timestamp" timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.locations OWNER TO postgres;

--
-- Name: messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.messages (
    id uuid NOT NULL,
    content character varying(5000) NOT NULL,
    delivered boolean NOT NULL,
    latitude double precision,
    longitude double precision,
    priority character varying(255),
    "timestamp" timestamp(6) without time zone NOT NULL,
    sender_id uuid NOT NULL,
    CONSTRAINT messages_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'EMERGENCY'::character varying])::text[])))
);


ALTER TABLE public.messages OWNER TO postgres;

--
-- Name: raw_emergency_requests; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.raw_emergency_requests (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(2000),
    latitude double precision,
    longitude double precision,
    priority character varying(255),
    type character varying(255),
    user_id uuid
);


ALTER TABLE public.raw_emergency_requests OWNER TO postgres;

--
-- Name: responders; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.responders (
    id uuid NOT NULL,
    latitude double precision,
    longitude double precision,
    name character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT responders_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'BUSY'::character varying, 'OFFLINE'::character varying])::text[]))),
    CONSTRAINT responders_type_check CHECK (((type)::text = ANY ((ARRAY['AMBULANCE'::character varying, 'FIRE_TRUCK'::character varying, 'POLICE_CAR'::character varying, 'RESCUE_TEAM'::character varying])::text[])))
);


ALTER TABLE public.responders OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    city character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    district character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    phone_number character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    username character varying(255) NOT NULL,
    password character varying(255) DEFAULT ''::character varying NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: emergency_requests emergency_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.emergency_requests
    ADD CONSTRAINT emergency_requests_pkey PRIMARY KEY (id);


--
-- Name: locations locations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT locations_pkey PRIMARY KEY (id);


--
-- Name: messages messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (id);


--
-- Name: raw_emergency_requests raw_emergency_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.raw_emergency_requests
    ADD CONSTRAINT raw_emergency_requests_pkey PRIMARY KEY (id);


--
-- Name: responders responders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.responders
    ADD CONSTRAINT responders_pkey PRIMARY KEY (id);


--
-- Name: users uk_9q63snka3mdh91as4io72espi; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_9q63snka3mdh91as4io72espi UNIQUE (phone_number);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: emergency_requests fk1kt0yy2sauibs2c8608dhb1jw; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.emergency_requests
    ADD CONSTRAINT fk1kt0yy2sauibs2c8608dhb1jw FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: messages fk4ui4nnwntodh6wjvck53dbk9m; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT fk4ui4nnwntodh6wjvck53dbk9m FOREIGN KEY (sender_id) REFERENCES public.users(id);


--
-- Name: locations fkdmtxqhxbtg4qt8q6u01hxj52i; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT fkdmtxqhxbtg4qt8q6u01hxj52i FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: emergency_requests fkljja0l9yhnbr23d5f2b475ylq; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.emergency_requests
    ADD CONSTRAINT fkljja0l9yhnbr23d5f2b475ylq FOREIGN KEY (assigned_responder_id) REFERENCES public.responders(id);


--
-- PostgreSQL database dump complete
--

\unrestrict 2cyb3c7zHRYAmNicIyHH9r2FgcvwerUblqUhORQS4zg0fYPfBVcBGn6RDLTuSCw

