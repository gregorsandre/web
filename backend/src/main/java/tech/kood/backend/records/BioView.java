package tech.kood.backend.records;

// What /users/{id}/bio answers with: the owner sees everything, others see BioWithTags
public sealed interface BioView permits OwnBio, BioWithTags {}
