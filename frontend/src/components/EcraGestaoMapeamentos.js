import React, { useState, useEffect, useCallback } from 'react';
import {
  getMappings,
  createMapping,
  updateMapping,
  deleteMapping,
  logAuditAction,
  reprocessEvents,
  resetEvents
} from '../services/categorizationService';
import './EcraGestaoMapeamentos.css';

const EcraGestaoMapeamentos = ({ onMappingChange }) => {
  const [mappings, setMappings] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editingAnterior, setEditingAnterior] = useState(null);
  const [formData, setFormData] = useState({ tipo_titulo: '', perfil: 'estudante' });

  const fetchMappings = useCallback(async (silencioso = false) => {
    try {
      const data = await getMappings();
      setMappings(data);
    } catch (err) {
      if (!silencioso) console.error('Erro a carregar mapeamentos', err);
    }
  }, []);

  useEffect(() => {
    let ativo = true;
    fetchMappings(false);

    const intervalId = setInterval(() => { if (ativo) fetchMappings(true); }, 3000);
    return () => { ativo = false; clearInterval(intervalId); };
  }, [fetchMappings]);

  const handleOpenModal = (mapping = null) => {
    if (mapping) {
      setFormData({ tipo_titulo: mapping.tipoTitulo, perfil: mapping.perfil });
      setEditingId(mapping.id);
      setEditingAnterior(mapping.perfil);
    } else {
      setFormData({ tipo_titulo: 'MENSAL', perfil: 'estudante' });
      setEditingId(null);
      setEditingAnterior(null);
    }
    setIsModalOpen(true);
  };

  const handleReset = async () => {
    if (window.confirm('TEM A CERTEZA? Isto vai apagar todos os mapeamentos e limpar todas as classificações da base de dados!')) {
      try {
        await resetEvents();
        await reprocessEvents();
        if (onMappingChange) onMappingChange();
        await fetchMappings(false);
        alert('Sistema reiniciado com sucesso.');
      } catch (err) {
        console.error('Erro ao reiniciar sistema', err);
      }
    }
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingId(null);
    setEditingAnterior(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await updateMapping(editingId, formData);
        await logAuditAction('UPDATE', formData.tipo_titulo, editingAnterior, formData.perfil);
      } else {
        await createMapping(formData);
        await logAuditAction('CREATE', formData.tipo_titulo, null, formData.perfil);
      }
      await reprocessEvents();
      if (onMappingChange) onMappingChange();
      await fetchMappings(false);
      handleCloseModal();
    } catch (err) {
      console.error('Erro a guardar mapeamento', err);
    }
  };

  const handleDelete = async (id, tipoTitulo, perfil) => {
    if (window.confirm(`Tem a certeza que deseja apagar o mapeamento para "${tipoTitulo}"?`)) {
      try {
        await deleteMapping(id);
        await logAuditAction('DELETE', tipoTitulo, perfil, null);
        await reprocessEvents();
        if (onMappingChange) onMappingChange();
        await fetchMappings(false);
      } catch (err) {
        console.error('Erro a apagar mapeamento', err);
      }
    }
  };

  return (
    <div className="gestao-mapeamentos-container">

      <div className="gestao-header">
        <h2>Mapeamentos de Tipologia</h2>
        <div className="header-actions">
          <button className="btn-secondary" onClick={handleReset}>
            Limpar Tudo
          </button>
          <button className="btn-primary" onClick={() => handleOpenModal()}>
            Adicionar mapeamento
          </button>
        </div>
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
                  <td>{m.tipoTitulo}</td>
                  <td>{m.perfil.charAt(0).toUpperCase() + m.perfil.slice(1)}</td>
                  <td>
                    <button className="btn-icon-edit" onClick={() => handleOpenModal(m)}>Editar</button>
                    <button className="btn-icon-delete" onClick={() => handleDelete(m.id, m.tipoTitulo, m.perfil)}>Apagar</button>
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
                <select
                  value={formData.tipo_titulo}
                  onChange={(e) => setFormData({ ...formData, tipo_titulo: e.target.value })}
                >
                  <option value="MENSAL">Mensal</option>
                  <option value="AVULSO">Avulso</option>
                  <option value="PASSE_ESTUDANTE">Estudante (Passe)</option>
                  <option value="PASSE_SENIOR">Sénior (Passe)</option>
                  <option value="PASSE_SOCIAL">Passe Social</option>
                </select>
              </div>
              <div className="form-group">
                <label>Perfil</label>
                <select
                  value={formData.perfil}
                  onChange={(e) => setFormData({ ...formData, perfil: e.target.value })}
                >
                  <option value="estudante">Estudante</option>
                  <option value="senior">Sénior</option>
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
