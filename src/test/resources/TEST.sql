--
-- PostgreSQL database dump
--
-- Dumped from database version 15.2
-- Dumped by pg_dump version 15.2
-- Started on 2023-03-08 16:31:29
CREATE DATABASE "TEST";

ALTER DATABASE "TEST" OWNER TO postgres;

\ connect "TEST" --
-- TOC entry 214 (class 1259 OID 16400)
-- Name: SINVOICE; Type: TABLE; Schema: public; Owner: postgres
--
CREATE TABLE public."SINVOICE" (
    "FCY_0" character varying(10),
    "CUR_0" character varying(5),
    "NUM_0" character varying(20)
);

ALTER TABLE
    public."SINVOICE" OWNER TO postgres;

--
-- TOC entry 3314 (class 0 OID 16400)
-- Dependencies: 214
-- Data for Name: SINVOICE; Type: TABLE DATA; Schema: public; Owner: postgres
--
INSERT INTO
    public."SINVOICE"
VALUES
    ('ZHU', 'RMB', 'ZCC2301001');

INSERT INTO
    public."SINVOICE"
VALUES
    ('ZHU', 'RMB', 'ZCC2301001');

INSERT INTO
    public."SINVOICE"
VALUES
    ('ZHU', 'RMB', 'ZCC2301002');

INSERT INTO
    public."SINVOICE"
VALUES
    ('ZHU', 'RMB', 'ZCC2301003');

-- Completed on 2023-03-08 16:31:29
--
-- PostgreSQL database dump complete
--