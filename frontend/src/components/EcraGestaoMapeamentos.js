import React, { useState, useEffect } from 'react';
import { getMappings, createMapping, deleteMapping, logAuditAction } from '../services/categorizationService';
import CategorizationStats from './CategorizationStats';
import './EcraGestaoMapeamentos.css';

const EcraGestaoMapeamentos = () => {
  const [mappings, setMappings] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({ tipo_titulo: '', perfil: 'estudante' });

  useEffect(() => {
    let ativo = true;

    const fetchMappings = async (silencioso = false) => {
      try {
        const data = await getMappings();
        if (ativo) setMappings(data);
      } catch (err) {
        if (!silencioso) console.error('Error fetching mappings', err);
      }
    };

    fetchMappings(false);

    const intervalId = setInterval(() => {
      if (ativo) {
        fetchMappings(true);
      }
    }, 3000);

    return () => {
      ativo = false;
      clearInterval(intervalId);
    };
  }, []);

  const auditAction = async (action, details) => {
    try {
      await fetch('/categorization/audit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action, details, timestamp: new Date().toISOString() })
      });
    } catch (err) {
      console.error('Default Audit error', err);
    }
  };

  const handleOpenModal = (mapping = null) => {
    if (mapping) {
      setFormData({ tipo_titulo: mapping.tipo_titulo, perfil: mapping.perfil });
      setEditingId(mapping.id);
    } else {
      setFormData({ tipo_titulo: '', perfil: 'estudante' });
      setEditingId(null);
    }
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        // Logic for update would go here
        await auditAction('EDIT', { id: editingId, ...formData });
      } else {
        await createMapping(formData);
        await auditAction('CREATE', formData);
      }
      handleCloseModal();
    } catch (err) {
      console.error('Error saving mapping', err);
    }
  };

  const handleDelete = async (id, tipo_titulo) => {
    if (window.confirm(`Tem a certeza que deseja apagar o mapeamento para "${tipo_titulo}"?`)) {
      try {
        await deleteMapping(id);
        await auditAction('DELETE', { id, tipo_titulo });
      } catch (err) {
        console.error('Error deleting', err);
      }
    }
  };

  return (
    <div className="gestao-mapeamentos-container">
      <CategorizationStats />
      
      <div className="gestao-header">
        <h2>Mapeamentos de Tipologia</h2>
        <button className="btn-primary" onClick={() => handleOpenModal()}>
          Adicionar mapeamento
        </button>
      </div>

      <div className="table-container">
        <table className="mappings-table">
          <thead>
            <tr>
              <th>Tipo de Título</th>
              <th>Perfil</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {mappings.length > 0 ? (
              mappings.map(m => (
                <tr key={m.id}>
                  <td>{m.tipo_titulo}</td>
                  <td>{m.perfil}</td>
                  <td>
                    <button className="btn-icon-edit" onClick={() => handleOpenModal(m)}>Editar</button>
                    <button className="btn-icon-delete" onClick={() => handleDelete(m.id, m.tipo_titulo)}>Apagar</button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="3" style={{ textAlign: 'center', color: '#64748b' }}>Sem mapeamentos definidos.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              {editingId ? 'Editar Mapeamento' : 'Novo Mapeamento'}
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Tipo de Título</label>
                <input 
                  type="text" 
                  value={formData.tipo_titulo} 
                  onChange={(e) => setFormData({...formData, tipo_titulo: e.target.value})}
                  required 
                />
              </div>
              <div className="form-group">
                <label>Perfil</label>
                <select 
                  value={formData.perfil} 
                  onChange={(e) => setFormData({...formData, perfil: e.target.value})}
                >
                  <option value="estudante">Estudante</option>
                  <option value="sénior">Sénior</option>
                  <option value="normal">Normal</option>
                  <option value="nao_categorizado">Não Categorizado</option>
                </select>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>Cancelar</button>
                <button type="submit" className="btn-primary">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default EcraGestaoMapeamentos;
